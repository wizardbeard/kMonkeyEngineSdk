/*
 * Copyright (c) 2009-2019 jMonkeyEngine All rights reserved. <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * <p>Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. <p/> <p>Redistributions
 * in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. <p/> <p>Neither the name of
 * 'jMonkeyEngine' nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission. <p/> <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.</p>
 */
package com.jme3.gde.core.scene.controller;

import com.jme3.app.state.AbstractAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.effect.ParticleEmitter;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.debug.WireBox;
import java.util.concurrent.Callable;

/**
 * This class can be used or extended by other plugins to display standard tools
 * in the tools scene e.g. a cursor etc.
 *
 * @author normenhansen
 */
public class SceneToolController extends AbstractAppState {

    protected Node toolsNode;
    protected boolean showSelection = false;
    protected boolean showGrid = false;
    protected Node cursor;
    protected Geometry grid;
    protected Spatial selected;
    protected Spatial selectionShape;
    protected AssetManager manager;
    protected Material blueMat;
    protected AbstractCameraController camController;

    private SceneToolControllerListener toolListener;
    
    /**
     * Expandable interface to send callbacks to (primarily) gui. 
     */
    public interface SceneToolControllerListener {
        
        /**
         * Called when cursor's location changes.
         *
         * @param location location for cursor
         */
        public void onSetCursorLocation(Vector3f location);
    }
    

    @SuppressWarnings("LeakingThisInConstructor")
    public SceneToolController(AssetManager manager) {
        this.toolsNode = new Node("ToolsNode");
        initTools();
        SceneApplication.getApplication().getStateManager().attach(this);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public SceneToolController(Node toolsNode, AssetManager manager) {
        this.toolsNode = toolsNode;
        this.manager = manager;
        initTools();
        SceneApplication.getApplication().getStateManager().attach(this);
    }

    public void setCamController(AbstractCameraController camController) {
        this.camController = camController;
    }

    protected final void initTools() {

        blueMat = createBlueMat();
        //Material redMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        //redMat.getAdditionalRenderState().setWireframe(true);
        //redMat.setColor("Color", ColorRGBA.Red);
        Material greenMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        greenMat.getAdditionalRenderState().setWireframe(true);
        greenMat.setColor("Color", ColorRGBA.Green);
        //Material blueMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        //blueMat.getAdditionalRenderState().setWireframe(true);
        //blueMat.setColor("Color", ColorRGBA.Blue);
        Material grayMat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        grayMat.getAdditionalRenderState().setWireframe(true);
        grayMat.setColor("Color", ColorRGBA.Gray);

        //cursor
        if (cursor == null) {
            cursor = new Node();
        }
        cursor.detachAllChildren();
        //Geometry cursorArrowX = new Geometry("cursorArrowX", new Arrow(Vector3f.UNIT_X));
        Geometry cursorArrowY = new Geometry("cursorArrowY", new Arrow(new Vector3f(0, -1, 0)));
        cursorArrowY.setLocalTranslation(0, 1, 0);
        //Geometry cursorArrowZ = new Geometry("cursorArrowZ", new Arrow(Vector3f.UNIT_Z));
        //cursorArrowX.setMaterial(redMat);
        cursorArrowY.setMaterial(greenMat);
        //cursorArrowZ.setMaterial(blueMat);
        //cursor.attachChild(cursorArrowX);
        cursor.attachChild(cursorArrowY);
        //cursor.attachChild(cursorArrowZ);

        //grid
        grid = new Geometry("grid", new Grid(21, 21, 1.0f));
        grid.setMaterial(grayMat);
        grid.center().move(Vector3f.ZERO);
        SceneApplication.getApplication().enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                toolsNode.attachChild(cursor);
                return null;
            }
        });
    }

    public void updateSelection(final Spatial spat) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                doUpdateSelection(spat);
                return null;
            }
        });
    }

    public void doUpdateSelection(Spatial spat) {
        if (showSelection && spat != null) {
            if (selected != spat) {
                if (selectionShape != null) {
                    detachSelectionShape();
                }
                attachSelectionShape(spat);
            } else {
                if (selectionShape == null) {
                    attachSelectionShape(spat);
                }
            }
        } else {
            if (selectionShape != null) {
                detachSelectionShape();
            }
        }
        selected = spat;
    }

    public void rebuildSelectionBox() {
        if (SceneApplication.getApplication().isAwt()) {
            SceneApplication.getApplication().enqueue(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    doUpdateSelection(selected);
                    return null;
                }
            });
        } else {
            if (selected != null) {
                attachSelectionShape(selected);
            }
        }
    }

    public void setCursorLocation(final Vector3f location) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                doSetCursorLocation(location);
                return null;
            }
        });
    }

    public void doSetCursorLocation(Vector3f location) {
        cursor.setLocalTranslation(location);
        if (camController != null) {
            camController.doSetCamFocus(location);
        }
        if (toolListener != null) {
            toolListener.onSetCursorLocation(location);
    }
    }

    public void snapCursorToSelection() {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                doSnapCursorToSelection();
                return null;
            }
        });
    }

    public void doSnapCursorToSelection() {
        if (selected != null) {
            cursor.setLocalTranslation(selected.getWorldTranslation());
        }
    }

    protected void attachSelectionShape(Spatial spat) {
        if (selectionShape != null) {
            selectionShape.removeFromParent();
            selectionShape = null;
        }
        if (spat instanceof ParticleEmitter) {
            attachBoxSelection(spat);

        } else if (spat instanceof Geometry) {
            attachGeometrySelection((Geometry) spat);
        } else if (spat.getControl(PhysicsControl.class) != null) {
            attachPhysicsSelection(spat);
        } else {
            attachBoxSelection(spat);
        }
    }

//    protected void attachParticleEmitterSelection(ParticleEmitter pe) {
//        Mesh mesh = pe.getMesh();
//        if (mesh == null) {
//            return;
//        }
//        Material mat = new Material(SceneApplication.getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//        mat.getAdditionalRenderState().setWireframe(true);
//        mat.setColor("Color", ColorRGBA.Blue);
//        pe.getWorldBound().
//        Geometry selectionGeometry = new Geometry("selection_geometry_sceneviewer", mesh);
//        selectionGeometry.setMaterial(mat);
//        selectionGeometry.setLocalTransform(pe.getWorldTransform());
//        toolsNode.attachChild(selectionGeometry);
//        selectionShape = selectionGeometry;
//    }
    protected void attachGeometrySelection(Geometry geom) {
        Mesh mesh = geom.getMesh();
        if (mesh == null) {
            return;
        }
        final Geometry selectionGeometry = new Geometry("selection_geometry_sceneviewer", mesh);
        selectionGeometry.setMaterial(blueMat);
        selectionGeometry.setLocalTransform(geom.getWorldTransform());
        selectionShape = selectionGeometry;
        SceneApplication.getApplication().enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                toolsNode.attachChild(selectionGeometry);
                return null;
            }
        });
    }

    protected void attachBoxSelection(Spatial geom) {
        BoundingVolume bound = geom.getWorldBound();
        if (bound instanceof BoundingBox) {
            BoundingBox bbox = (BoundingBox) bound;
            Vector3f extent = new Vector3f();
            bbox.getExtent(extent);
            final Geometry selectionGeometry = WireBox.makeGeometry(bbox);
            selectionGeometry.setName("selection_geometry_sceneviewer");
            selectionGeometry.setMaterial(blueMat);
            selectionGeometry.setLocalTranslation(bbox.getCenter().subtract(geom.getWorldTranslation()));
            //Vector3f scale = new Vector3f(1,1,1);
            //scale.x = 1/geom.getWorldScale().x;
            //scale.y = 1/geom.getWorldScale().y;
            //scale.z = 1/geom.getWorldScale().z;
            selectionShape = new Node("SelectionParent");
            ((Node) selectionShape).attachChild(selectionGeometry);
            //selectionShape.setLocalTransform(geom.getWorldTransform());
            //selectionShape.setLocalTranslation(geom.getWorldTranslation());
            //selectionGeometry.setLocalScale(scale);

            SceneApplication.getApplication().enqueue(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    toolsNode.attachChild(selectionShape);
                    return null;
                }
            });

        }
    }

    private Material createBlueMat() {
        Material mat = new Material(SceneApplication.getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Blue);
        return mat;
    }

    protected void attachPhysicsSelection(Spatial geom) {
        PhysicsCollisionObject control = geom.getControl(RigidBodyControl.class);
        if (control == null) {
            control = geom.getControl(VehicleControl.class);
        }
        if (control == null) {
            control = geom.getControl(GhostControl.class);
        }
        if (control == null) {
            control = geom.getControl(CharacterControl.class);
        }
        if (control == null) {
            return;
        }
        final Spatial selectionGeometry = DebugShapeFactory.getDebugShape(control.getCollisionShape());
        if (selectionGeometry != null) {
            selectionGeometry.setMaterial(blueMat);
            selectionGeometry.setLocalTransform(geom.getWorldTransform());
            selectionShape = selectionGeometry;
            SceneApplication.getApplication().enqueue(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    toolsNode.attachChild(selectionGeometry);
                    return null;
                }
            });
        }
    }

    protected void detachSelectionShape() {
        if (selectionShape != null) {
            final Spatial shape = selectionShape;
            selectionShape = null;
            SceneApplication.getApplication().enqueue(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    shape.removeFromParent();
                    return null;
                }
            });
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        detachSelectionShape();
        final Spatial cursor = this.cursor;
        final Spatial grid = this.grid;
        SceneApplication.getApplication().enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                cursor.removeFromParent();
                grid.removeFromParent();
                return null;
            }
        });
        SceneApplication.getApplication().getStateManager().detach(this);
    }

    //TODO: multithreading!
    public Vector3f getCursorLocation() {
        return cursor.getLocalTranslation();
    }

    public void setShowSelection(final boolean showSelection) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                doSetShowSelection(showSelection);
                return null;
            }
        });
    }

    public void doSetShowSelection(boolean showSelection) {
        this.showSelection = showSelection;
        if (showSelection && selected != null && selectionShape == null) {
            attachSelectionShape(selected);
        } else if (!showSelection && selectionShape != null) {
            detachSelectionShape();
        }
    }

    public void setShowGrid(final boolean showGrid) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                doSetShowGrid(showGrid);
                return null;
            }
        });
    }

    public void doSetShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        if (showGrid) {
            toolsNode.attachChild(grid);
        } else {
            toolsNode.detachChild(grid);
        }
    }

    /**
     * @return the toolsNode
     */
    public Node getToolsNode() {
        return toolsNode;
    }

    @Override
    public void update(float f) {
        if (selected == null || selectionShape == null) {
            return;
        }

        selectionShape.setLocalTranslation(selected.getWorldTranslation());
        selectionShape.setLocalRotation(selected.getWorldRotation());
        selectionShape.setLocalScale(selected.getWorldScale());

    }

    public Spatial getSelectedSpatial() {
        return selected;
    }

    public Spatial getSelectionShape() {
        return selectionShape;
    }
    
    /**
     * Set the listener to receive callbacks from this class.
     * 
     * @param listener listener
     */
    public void setToolListener(final SceneToolControllerListener listener) {
        this.toolListener = listener;
    }
}
