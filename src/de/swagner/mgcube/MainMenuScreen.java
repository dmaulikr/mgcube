package de.swagner.mgcube;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.loaders.obj.ObjLoader;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;

import de.swagner.gdx.obj.normalmap.helper.ObjLoaderTan;
import de.swagner.gdx.obj.normalmap.shader.BloomShader;
import de.swagner.gdx.obj.normalmap.shader.Quad2Shader;
import de.swagner.gdx.obj.normalmap.shader.TransShader;

public class MainMenuScreen extends DefaultScreen implements InputProcessor {

	float startTime = 0;
	PerspectiveCamera cam;
	Mesh quadModel;
	Mesh blockModel;
	Mesh playerModel;
	Mesh targetModel;
	Mesh worldModel;
	Mesh wireCubeModel;
	float angleX = 0;
	float angleY = 0;
	SpriteBatch batch;
	BitmapFont font;
	SpriteBatch fadeBatch;
	Sprite blackFade;
	Sprite title;
	float fade = 1.0f;
	boolean finished = false;
	
	Player player = new Player();
	Target target = new Target();
	Array<Block> blocks = new Array<Block>();
	boolean animateWorld = false;
	boolean animatePlayer = false;	

	Vector3 xAxis = new Vector3(1, 0, 0);
	Vector3 yAxis = new Vector3(0, 1, 0);
	Vector3 zAxis = new Vector3(0, 0, 1);	

	// GLES20
	Matrix4 model = new Matrix4().idt();
	Matrix4 modelView = new Matrix4().idt();
	Matrix4 modelViewProjection = new Matrix4().idt();
	Matrix4 tmp = new Matrix4().idt();
	private ShaderProgram transShader;
	private ShaderProgram bloomShader;
	FrameBuffer frameBuffer;
	FrameBuffer frameBuffer1;
	Texture fbTexture;
	Texture texture;

	public MainMenuScreen(Game game) {
		super(game);
		Gdx.input.setInputProcessor(this);
		
		title = new Sprite(new Texture(Gdx.files.internal("data/title.png")));
		blackFade = new Sprite(new Texture(Gdx.files.internal("data/blackfade.png")));

		blockModel = ObjLoader.loadObj(Gdx.files.internal("data/cube.obj").read());
		blockModel.getVertexAttribute(Usage.Position).alias = "a_vertex";

		playerModel = ObjLoader.loadObj(Gdx.files.internal("data/sphere.obj").read());
		playerModel.getVertexAttribute(Usage.Position).alias = "a_vertex";

		targetModel = ObjLoader.loadObj(Gdx.files.internal("data/cylinder.obj").read());
		targetModel.getVertexAttribute(Usage.Position).alias = "a_vertex";


		quadModel = new Mesh(true, 4, 6, new VertexAttribute(Usage.Position, 4, "a_position"), new VertexAttribute(Usage.TextureCoordinates, 2, "a_texCoord"));
		float[] vertices = { -1.0f, 1.0f, 0.0f, 1.0f, // Position 0
				0.0f, 0.0f, // TexCoord 0
				-1.0f, -1.0f, 0.0f, 1.0f, // Position 1
				0.0f, 1.0f, // TexCoord 1
				1.0f, -1.0f, 0.0f, 1.0f, // Position 2
				1.0f, 1.0f, // TexCoord 2
				1.0f, 1.0f, 0.0f, 1.0f, // Position 3
				1.0f, 0.0f // TexCoord 3
		};
		short[] indices = { 0, 1, 2, 0, 2, 3 };
		quadModel.setVertices(vertices);
		quadModel.setIndices(indices);

		wireCubeModel = new Mesh(true, 20, 20, new VertexAttribute(Usage.Position, 4, "a_vertex"));
		float[] vertices2 = {
				// front face
				-1.0f, 1.0f, 1.0f, 1.0f, // 0
				1.0f, 1.0f, 1.0f, 1.0f, // 1
				1.0f, -1.0f, 1.0f, 1.0f, // 2
				-1.0f, -1.0f, 1.0f, 1.0f, // 3

				// left face
				-1.0f, 1.0f, 1.0f, 1.0f, // 0
				-1.0f, 1.0f, -1.0f, 1.0f, // 4
				-1.0f, -1.0f, -1.0f, 1.0f, // 7
				-1.0f, -1.0f, 1.0f, 1.0f, // 3

				// bottom face
				-1.0f, -1.0f, 1.0f, 1.0f, // 3
				1.0f, -1.0f, 1.0f, 1.0f, // 2
				1.0f, -1.0f, -1.0f, 1.0f, // 6
				-1.0f, -1.0f, -1.0f, 1.0f, // 7

				// back face
				-1.0f, -1.0f, -1.0f, 1.0f, // 7
				-1.0f, 1.0f, -1.0f, 1.0f, // 4
				1.0f, 1.0f, -1.0f, 1.0f, // 5
				1.0f, -1.0f, -1.0f, 1.0f, // 6

				// right face
				1.0f, -1.0f, -1.0f, 1.0f, // 6
				1.0f, -1.0f, 1.0f, 1.0f, // 2
				1.0f, 1.0f, 1.0f, 1.0f, // 1
				1.0f, 1.0f, -1.0f, 1.0f, // 5
		};
		short[] indices2 = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 };
		wireCubeModel.setVertices(vertices2);
		wireCubeModel.setIndices(indices2);

		cam = new PerspectiveCamera(60, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(0, 0, 16f);
		cam.direction.set(0, 0, -1);
		cam.up.set(0, 1, 0);
		cam.near = 1f;
		cam.far = 1000;

		// controller = new PerspectiveCamController(cam);
		// Gdx.input.setInputProcessor(controller);

		batch = new SpriteBatch();
		batch.getProjectionMatrix().setToOrtho2D(0, 0, 800, 480);
		font = new BitmapFont();		

		fadeBatch = new SpriteBatch();
		fadeBatch.getProjectionMatrix().setToOrtho2D(0, 0, 2, 2);

		initShader();
		initLevel(1);
		initRender();
	}

	private void initShader() {
		transShader = new ShaderProgram(TransShader.mVertexShader, TransShader.mFragmentShader);
		if (transShader.isCompiled() == false) {
			Gdx.app.log("ShaderTest", transShader.getLog());
			System.exit(0);
		}

		bloomShader = new ShaderProgram(BloomShader.mVertexShader, BloomShader.mFragmentShader);
		if (bloomShader.isCompiled() == false) {
			Gdx.app.log("ShaderTest", bloomShader.getLog());
			System.exit(0);
		}
	}

	private void initRender() {
		Gdx.graphics.getGL20().glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		Gdx.graphics.getGL20().glClearColor(0.4f, 0.4f, 0.4f, 1.0f);

		Gdx.graphics.getGL20().glEnable(GL20.GL_BLEND);
		Gdx.graphics.getGL20().glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		frameBuffer = new FrameBuffer(Format.RGB565, 800, 480, false);
		frameBuffer1 = new FrameBuffer(Format.RGB565, 800, 480, false);
	}

	private void initLevel(int levelnumber) {
		blocks.clear();
		int[][][] level;
		switch (levelnumber) {
		case 1:
			level = Resources.getInstance().level1;
			break;
		case 2:
			level = Resources.getInstance().level2;
			break;
		case 3:
			level = Resources.getInstance().level3;
			break;

		// more levels

		default:
			level = Resources.getInstance().level1;
			break;
		}

		// finde player pos
		int z = 0, y = 0, x = 0;
		for (z = 0; z < 10; z++) {
			for (y = 0; y < 10; y++) {
				for (x = 0; x < 10; x++) {
					if (level[z][y][x] == 1) {
						blocks.add(new Block(new Vector3(-10f + (x * 2), -10f + (y * 2), -10f + (z * 2))));
					}
					if (level[z][y][x] == 2) {
						player.position.x = -10f + (x * 2);
						player.position.y = -10f + (y * 2);
						player.position.z = -10f + (z * 2);
					}
					if (level[z][y][x] == 3) {
						target.position.x = -10f + (x * 2);
						target.position.y = -10f + (y * 2);
						target.position.z = -10f + (z * 2);
					}
				}
			}
		}
	}
	
	@Override
	public void show() {
	}

	@Override
	public void render(float delta) {
		startTime += delta;

		angleX += MathUtils.sin(startTime);
		angleY += MathUtils.cos(startTime);
		
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		frameBuffer.begin();
		Gdx.graphics.getGL20().glViewport(0, 0, frameBuffer.getWidth(), frameBuffer.getHeight());

		Gdx.graphics.getGL20().glEnable(GL20.GL_BLEND);
		Gdx.graphics.getGL20().glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		cam.update();

		Gdx.gl.glEnable(GL20.GL_CULL_FACE);
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
	
		// render Blocks
		for (Block block : blocks) {
			tmp.idt();
			model.idt();
			modelView.idt();

			tmp.setToScaling(0.5f, 0.5f, 0.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, angleX);
			model.mul(tmp);
			tmp.setToRotation(yAxis, angleY);
			model.mul(tmp);

			// modelView.set(cam.view);
			// modelView.mul(model);
			// tmp.setToRotation(angleY, modelView.getValues()[1],
			// modelView.getValues()[5], modelView.getValues()[9]);
			// model.mul(tmp);
			//
			// modelView.set(cam.view);
			// modelView.mul(model);
			// tmp.setToRotation(angleX, modelView.getValues()[0],
			// modelView.getValues()[4], modelView.getValues()[8]);
			// model.mul(tmp);
			//
			tmp.setToTranslation(block.position.x, block.position.y, block.position.z);
			model.mul(tmp);

			tmp.setToScaling(0.95f, 0.95f, 0.95f);
			model.mul(tmp);

			transShader.begin();

			modelViewProjection.idt();
			modelViewProjection.set(cam.combined);
			modelViewProjection = tmp.mul(model);

			transShader.setUniformMatrix("MVPMatrix", modelViewProjection);

			transShader.setUniformf("a_color", 1.0f, 0.1f, 0.1f);
			transShader.setUniformf("alpha", 0.8f);
			wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);

			transShader.setUniformf("a_color", 1.0f, 0.1f, 0.1f);
			transShader.setUniformf("alpha", 0.2f);
			blockModel.render(transShader, GL20.GL_TRIANGLES);

			transShader.end();
		}

		{
			// render Wire
			tmp.idt();
			model.idt();
			modelView.idt();

			tmp.setToScaling(5.5f, 5.5f, 5.5f);
			model.mul(tmp);

			tmp.setToRotation(xAxis, angleX);
			model.mul(tmp);
			tmp.setToRotation(yAxis, angleY);
			model.mul(tmp);

			// modelView.set(cam.view);
			// modelView.mul(model);
			// tmp.setToRotation(angleY, modelView.getValues()[1],
			// modelView.getValues()[5], modelView.getValues()[9]);
			// model.mul(tmp);
			//
			// modelView.set(cam.view);
			// modelView.mul(model);
			// tmp.setToRotation(angleX, modelView.getValues()[0],
			// modelView.getValues()[4], modelView.getValues()[8]);
			// model.mul(tmp);
			//
			tmp.setToTranslation(0, 0, 0);
			model.mul(tmp);

			transShader.begin();

			modelViewProjection.idt();
			modelViewProjection.set(cam.combined);
			modelViewProjection = tmp.mul(model);

			transShader.setUniformMatrix("MVPMatrix", modelViewProjection);
			// shader.setUniformf("LightDirection", light.x, light.y, light.z);

			transShader.setUniformf("a_color", 1.0f, 0.1f, 0.1f);
			transShader.setUniformf("alpha", 0.2f);
			wireCubeModel.render(transShader, GL20.GL_LINE_STRIP);

			transShader.setUniformf("a_color", 1.0f, 0.1f, 0.1f);
			transShader.setUniformf("alpha", 0.09f);
			blockModel.render(transShader, GL20.GL_TRIANGLES);

			transShader.end();
		}
		

		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		
	
		frameBuffer.end();
		
		Gdx.graphics.getGL20().glDisable(GL20.GL_BLEND);

		frameBuffer.getColorBufferTexture().bind(0);

		frameBuffer1.begin();
		Gdx.graphics.getGL20().glViewport(0, 0, frameBuffer1.getWidth(), frameBuffer1.getHeight());
		
		bloomShader.begin();
		batch.getProjectionMatrix().setToOrtho2D(0, 0, frameBuffer1.getWidth(), frameBuffer1.getHeight());
		bloomShader.setUniformi("s_texture", 0);
		bloomShader.setUniformf("bloomfactor", (MathUtils.sin(startTime * 5f) * 0.1f) + 1.0f);
		quadModel.render(bloomShader, GL20.GL_TRIANGLE_STRIP);
		bloomShader.end(); 
		frameBuffer1.end();

		Gdx.graphics.getGL20().glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		batch.begin();
		batch.getProjectionMatrix().setToOrtho2D(0, 0, 800, 480);
		batch.draw(frameBuffer1.getColorBufferTexture(), 0, 0);
		batch.end();
		
		batch.begin();
		batch.getProjectionMatrix().setToOrtho2D(0, 0, 800, 480);
		batch.draw(title, 40, 370);
		batch.end();
		
		if (!finished && fade > 0) {
			fade = Math.max(fade - Gdx.graphics.getDeltaTime() / 2.f, 0);
			fadeBatch.begin();
			blackFade.setColor(blackFade.getColor().r, blackFade.getColor().g, blackFade.getColor().b, fade);
			blackFade.draw(fadeBatch);
			fadeBatch.end();
		}

		if (finished) {
			fade = Math.min(fade + Gdx.graphics.getDeltaTime() / 2.f, 1);
			fadeBatch.begin();
			blackFade.setColor(blackFade.getColor().r, blackFade.getColor().g, blackFade.getColor().b, fade);
			blackFade.draw(fadeBatch);
			fadeBatch.end();
			if (fade >= 1) {
				game.setScreen(new GameScreen(game));
			}
		}

	}

	@Override
	public void hide() {
	}

	@Override
	public boolean keyDown(int keycode) {
		if (Gdx.input.isTouched())
			return false;

		if (keycode == Input.Keys.SPACE) {
			finished = true;
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		x = (int) (x / (float) Gdx.graphics.getWidth() * 800);
		y = (int) (y / (float) Gdx.graphics.getHeight() * 480);

		finished = true;

		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		x = (int) (x / (float) Gdx.graphics.getWidth() * 800);
		y = (int) (y / (float) Gdx.graphics.getHeight() * 480);

		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		x = (int) (x / (float) Gdx.graphics.getWidth() * 800);
		y = (int) (y / (float) Gdx.graphics.getHeight() * 480);

		return false;
	}

	@Override
	public boolean touchMoved(int x, int y) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
			cam.translate(0, 0, 1 * amount);
		if((cam.position.z < 2 && amount < -0) || (cam.position.z > 20 && amount > 0))
			cam.translate(0, 0, 1 * -amount);
		return false;
	}

}