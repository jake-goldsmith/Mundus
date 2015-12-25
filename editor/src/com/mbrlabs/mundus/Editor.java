/*
 * Copyright (c) 2015. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbrlabs.mundus;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.mbrlabs.mundus.core.HomeManager;
import com.mbrlabs.mundus.core.Scene;
import com.mbrlabs.mundus.core.project.ProjectManager;
import com.mbrlabs.mundus.core.project.ProjectRef;
import com.mbrlabs.mundus.events.EventBus;
import com.mbrlabs.mundus.terrain.TerrainInstance;
import com.mbrlabs.mundus.core.Inject;
import com.mbrlabs.mundus.core.Mundus;
import com.mbrlabs.mundus.shader.Shaders;
import com.mbrlabs.mundus.core.project.ProjectContext;
import com.mbrlabs.mundus.input.InputManager;
import com.mbrlabs.mundus.input.navigation.FreeCamController;
import com.mbrlabs.mundus.terrain.Terrain;
import com.mbrlabs.mundus.tools.ToolManager;
import com.mbrlabs.mundus.ui.Ui;
import com.mbrlabs.mundus.utils.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class Editor implements ApplicationListener {

    // axes
    private RenderContext renderContext;
    private ModelInstance axesInstance;

    private Ui ui;
    private Compass compass;
    private FreeCamController camController;

    @Inject
    private InputManager inputManager;
    @Inject
    private PerspectiveCamera cam;
    @Inject
    private ModelBatch batch;
    @Inject
    private ProjectContext projectContext;
    @Inject
    private Shaders shaders;
    @Inject
    private ProjectManager projectManager;
    @Inject
    private HomeManager homeManager;
    @Inject
    private EventBus eventBus;
    @Inject
    private ToolManager toolManager;

	@Override
	public void create () {
        Mundus.init();
        Mundus.inject(this);
        ui = Ui.getInstance();
        inputManager.addProcessor(ui);
        camController = new FreeCamController(cam);
        inputManager.addProcessor(camController);
        compass = new Compass(cam);


        Model axesModel = UsefulMeshs.createAxes();
        axesInstance = new ModelInstance(axesModel);
        Mundus.testModels.add(axesModel);

        renderContext = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.WEIGHTED, 1));

        // open last edited project or create default project
        boolean projectOpened = projectManager.openLastOpenedProject();
        if(!projectOpened) {
            createDefaultProject();
        }

        createTestModels();
    }

	@Override
	public void render () {
        GlUtils.clearScreen(Color.WHITE);

        ui.act();
        camController.update();
        toolManager.act();

        // update status bar
        ui.getStatusBar().setFps(Gdx.graphics.getFramesPerSecond());
        ui.getStatusBar().setVertexCount(0);

        // render model instances
       batch.begin(cam);
       batch.render(axesInstance);
//       batch.render(projectContext.entities,
//                projectContext.environment, shaders.entityShader);
       batch.render(Mundus.testInstances,
                projectContext.currScene.environment, shaders.entityShader);
       batch.end();

       // render terrains
       shaders.terrainShader.begin(cam, renderContext);
       for(TerrainInstance terrain : projectContext.currScene.terrainGroup.getTerrains()) {
           terrain.terrain.renderable.environment = projectContext.currScene.environment;
           terrain.terrain.renderable.worldTransform.set(terrain.transform);
           shaders.terrainShader.render(terrain.terrain.renderable);
       }
       shaders.terrainShader.end();

       toolManager.render();

        // render compass
        compass.render(batch);

        // render UI
        ui.draw();
	}

    @Deprecated
    private void createTestModels() {
        // boxes to test terrain height
        if(projectContext.currScene.terrainGroup.size() > 0) {
            float boxSize = 0.5f;
            Model boxModel = new ModelBuilder().createBox(boxSize, boxSize,boxSize,
                    new Material(ColorAttribute.createDiffuse(Color.RED)),
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            Mundus.testModels.add(boxModel);
            Mundus.testInstances.addAll(TestUtils.createABunchOfModelsOnTheTerrain(1000,
                    boxModel, projectContext.currScene.terrainGroup.first()));
        }
    }

    private void createDefaultProject() {
        if(homeManager.homeDescriptor.lastProject == null || homeManager.homeDescriptor.projects.size() == 0) {
            String name = "Default Project";
            String path = FileUtils.getUserDirectoryPath();
            path = FilenameUtils.concat(path, "MundusProjects");

            ProjectContext project = projectManager.createProject(name, path);
            projectManager.changeProject(project);
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void resize(int width, int height) {
        ui.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        Mundus.dispose();
    }

}
