package mx.itson.cmcarracing;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.andengine.audio.music.Music;
import org.andengine.audio.music.MusicFactory;
import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.sprite.TiledSprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.FixedStepPhysicsWorld;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.debug.Debug;
import org.andengine.util.math.MathUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga
 * (c) 2015 Franco, B.; Parra, M.; Calderon, R. F.; Ramos, J. M.
 *
 * @author Franco, B.; Parra, M.; Calderon, R. F.; Ramos, J. M.
 * @since 22:43:20 - 15.07.2010
 */
public class RacerGameActivity extends SimpleBaseGameActivity{
    // ===========================================================
    // Constants
    // ===========================================================
    private static final int RACETRACK_WIDTH = 64;

    private static final int OBSTACLE_SIZE = 16;
    private static final int CAR_SIZE = 16;

    private static final int CAMERA_WIDTH = RACETRACK_WIDTH * 5;
    private static final int CAMERA_HEIGHT = RACETRACK_WIDTH * 3;


    // ===========================================================
    // Fields
    // ===========================================================

    private Camera mCamera;

    private BitmapTextureAtlas mVehiclesTexture;
    private TiledTextureRegion mVehiclesTextureRegion;

    private BitmapTextureAtlas mBoxTexture;
    private ITextureRegion mBoxTextureRegion;

    private BitmapTextureAtlas mRacetrackTexture;
    private ITextureRegion mRacetrackStraightTextureRegion;
    private ITextureRegion mRacetrackCurveTextureRegion;

    private BitmapTextureAtlas mOnScreenControlTexture;
    private ITextureRegion mOnScreenControlBaseTextureRegion;
    private ITextureRegion mOnScreenControlKnobTextureRegion;

    private Scene mScene;

    private PhysicsWorld mPhysicsWorld;

    private Body mCarBody;
    private TiledSprite mCar;
    private int indexCar;

    private TreeMap<Integer,Body> listaCarritos =  new TreeMap<Integer, Body>();
    private TreeMap<Integer, Double> listaScore = new TreeMap<Integer, Double>();

    private Music mMusic;
    private Music bg1;
    private Music bg2;
    private Music bg3;
    private Music bg4;
    private Music bg5;

    private Sound punch;
    private Sound smashing;


    // FUENTE

    private Font gameFont;

    private Text ip;

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public EngineOptions onCreateEngineOptions() {
        this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
        turnOnOffHotspot(getApplicationContext(),true);
        Toast.makeText(getApplicationContext(),"En linea",Toast.LENGTH_LONG).show();
        EngineOptions eo = new  EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
        eo.getAudioOptions().setNeedsMusic(true).setNeedsSound(true);

        return eo;
    }

    @Override
    public void onCreateResources() {
        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        // Inicializar fuentes
        gameFont= FontFactory.create(this.getFontManager(), this.getTextureManager(),256,256, Typeface.create(Typeface.DEFAULT,Typeface.BOLD_ITALIC),24, Color.WHITE);
        gameFont.load();

        this.mVehiclesTexture = new BitmapTextureAtlas(this.getTextureManager(), 128, 16, TextureOptions.BILINEAR);
        this.mVehiclesTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mVehiclesTexture, this, "vehicles.png", 0, 0, 6, 1);
        this.mVehiclesTexture.load();

        this.mRacetrackTexture = new BitmapTextureAtlas(this.getTextureManager(), 128, 256, TextureOptions.REPEATING_NEAREST);
        this.mRacetrackStraightTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mRacetrackTexture, this, "racetrack_straight.png", 0, 0);
        this.mRacetrackCurveTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mRacetrackTexture, this, "racetrack_curve.png", 0, 128);
        this.mRacetrackTexture.load();

        this.mOnScreenControlTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
        this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_base.png", 0, 0);
        this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_knob.png", 128, 0);
        this.mOnScreenControlTexture.load();

        this.mBoxTexture = new BitmapTextureAtlas(this.getTextureManager(), 32, 32, TextureOptions.BILINEAR);
        this.mBoxTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBoxTexture, this, "box.png", 0, 0);
        this.mBoxTexture.load();

        SoundFactory.setAssetBasePath("mfx/");
        try {
            punch = SoundFactory.createSoundFromAsset(getEngine().getSoundManager(), this, "punch.ogg");
            smashing = SoundFactory.createSoundFromAsset(getEngine().getSoundManager(), this, "smashing.ogg");
        } catch (final IOException e) {
            Debug.e(e);
        }

        MusicFactory.setAssetBasePath("mfx/");
        try {
            bg1 = MusicFactory.createMusicFromAsset(getEngine().getMusicManager(), this, "bg1.ogg");
            bg2 = MusicFactory.createMusicFromAsset(getEngine().getMusicManager(), this, "bg2.ogg");
            bg3 = MusicFactory.createMusicFromAsset(getEngine().getMusicManager(), this, "bg3.ogg");
            bg4 = MusicFactory.createMusicFromAsset(getEngine().getMusicManager(), this, "bg4.ogg");
            bg5 = MusicFactory.createMusicFromAsset(getEngine().getMusicManager(), this, "bg5.ogg");
            //mMusic.setLooping(true);
        } catch (final IOException e) {
            Debug.e(e);
        }
    }


    @Override
    public Scene onCreateScene() {
        this.mEngine.registerUpdateHandler(new FPSLogger());

        this.mScene = new Scene();
        this.mScene.setBackground(new Background(0, 0, 0));

        this.mPhysicsWorld = new FixedStepPhysicsWorld(30, new Vector2(0, 0), false, 8, 1);

        this.initRacetrack();
        this.initRacetrackBorders();
        this.initObstacles();

        this.mPhysicsWorld.setContactListener(this.createContactListener());
        this.mScene.registerUpdateHandler(this.mPhysicsWorld);
        String haha=getIpAddress();
        ip = new Text(0,0,gameFont,haha,100,this.getVertexBufferObjectManager());
        return this.mScene;
    }

    @Override
    public void onGameCreated() {
        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
    }


    private void initCar(int tag) {
        try {
            this.mCar = new TiledSprite(20, 20, CAR_SIZE, CAR_SIZE, this.mVehiclesTextureRegion, this.getVertexBufferObjectManager());
            this.mCar.setCurrentTileIndex(indexCar);
            this.mCar.setTag(tag);

            final FixtureDef carFixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);
            this.mCarBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, this.mCar, BodyType.DynamicBody, carFixtureDef);
            this.mCarBody.setUserData(new UserDataCar(tag,"car"));


            this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(this.mCar, this.mCarBody, true, false));

            this.listaCarritos.put(tag, mCarBody);
            this.mScene.attachChild(this.mCar);
        }catch(NullPointerException e){
            Toast.makeText(getApplicationContext(),"ERROR, reiniciar el juego por favor. "+e.getMessage(),Toast.LENGTH_LONG);
        }
    }

    private int randomCar(int tag){
        Random rn = new Random();
        rn.setSeed(tag);

        return rn.nextInt(5);
    }

    private void initObstacles() {
        this.addObstacle(CAMERA_WIDTH / 2, RACETRACK_WIDTH / 2);
        this.addObstacle(CAMERA_WIDTH / 2, RACETRACK_WIDTH / 2);
        this.addObstacle(CAMERA_WIDTH / 2, CAMERA_HEIGHT - RACETRACK_WIDTH / 2);
        this.addObstacle(CAMERA_WIDTH / 2, CAMERA_HEIGHT - RACETRACK_WIDTH / 2);
    }

    private void addObstacle(final float pX, final float pY) {
        final Sprite box = new Sprite(pX, pY, OBSTACLE_SIZE, OBSTACLE_SIZE, this.mBoxTextureRegion, this.getVertexBufferObjectManager());
        final FixtureDef boxFixtureDef = PhysicsFactory.createFixtureDef(0.1f, 0.5f, 0.5f);
        box.setUserData("bax");
        final Body boxBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, box, BodyType.DynamicBody, boxFixtureDef);
        boxBody.setLinearDamping(10);
        boxBody.setAngularDamping(10);
        boxBody.setUserData("bax");

        this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(box, boxBody, true, true));

        this.mScene.attachChild(box);
    }

    private void initRacetrack() {
		/* Straights. */
        {
            final ITextureRegion racetrackHorizontalStraightTextureRegion = this.mRacetrackStraightTextureRegion.deepCopy();
            racetrackHorizontalStraightTextureRegion.setTextureWidth(3 * this.mRacetrackStraightTextureRegion.getWidth());

            final ITextureRegion racetrackVerticalStraightTextureRegion = this.mRacetrackStraightTextureRegion;

			/* Top Straight */
            this.mScene.attachChild(new Sprite(RACETRACK_WIDTH, 0, 3 * RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackHorizontalStraightTextureRegion, this.getVertexBufferObjectManager()));
			/* Bottom Straight */
            this.mScene.attachChild(new Sprite(RACETRACK_WIDTH, CAMERA_HEIGHT - RACETRACK_WIDTH, 3 * RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackHorizontalStraightTextureRegion, this.getVertexBufferObjectManager()));

			/* Left Straight */
            final Sprite leftVerticalStraight = new Sprite(0, RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackVerticalStraightTextureRegion, this.getVertexBufferObjectManager());
            leftVerticalStraight.setRotation(90);
            this.mScene.attachChild(leftVerticalStraight);
			/* Right Straight */
            final Sprite rightVerticalStraight = new Sprite(CAMERA_WIDTH - RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackVerticalStraightTextureRegion, this.getVertexBufferObjectManager());
            rightVerticalStraight.setRotation(90);
            this.mScene.attachChild(rightVerticalStraight);
        }

		/* Edges */
        {
            final ITextureRegion racetrackCurveTextureRegion = this.mRacetrackCurveTextureRegion;

			/* Upper Left */
            final Sprite upperLeftCurve = new Sprite(0, 0, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
            upperLeftCurve.setRotation(90);
            this.mScene.attachChild(upperLeftCurve);

			/* Upper Right */
            final Sprite upperRightCurve = new Sprite(CAMERA_WIDTH - RACETRACK_WIDTH, 0, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
            upperRightCurve.setRotation(180);
            this.mScene.attachChild(upperRightCurve);

			/* Lower Right */
            final Sprite lowerRightCurve = new Sprite(CAMERA_WIDTH - RACETRACK_WIDTH, CAMERA_HEIGHT - RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
            lowerRightCurve.setRotation(270);
            this.mScene.attachChild(lowerRightCurve);

			/* Lower Left */
            final Sprite lowerLeftCurve = new Sprite(0, CAMERA_HEIGHT - RACETRACK_WIDTH, RACETRACK_WIDTH, RACETRACK_WIDTH, racetrackCurveTextureRegion, this.getVertexBufferObjectManager());
            this.mScene.attachChild(lowerLeftCurve);
        }
    }


    private void initRacetrackBorders() {
        final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();

        final Rectangle bottomOuter = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2, vertexBufferObjectManager);
        final Rectangle topOuter = new Rectangle(0, 0, CAMERA_WIDTH, 2, vertexBufferObjectManager);
        final Rectangle leftOuter = new Rectangle(0, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
        final Rectangle rightOuter = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);

        final Rectangle bottomInner = new Rectangle(RACETRACK_WIDTH, CAMERA_HEIGHT - 2 - RACETRACK_WIDTH, CAMERA_WIDTH - 2 * RACETRACK_WIDTH, 2, vertexBufferObjectManager);
        final Rectangle topInner = new Rectangle(RACETRACK_WIDTH, RACETRACK_WIDTH, CAMERA_WIDTH - 2 * RACETRACK_WIDTH, 2, vertexBufferObjectManager);
        final Rectangle leftInner = new Rectangle(RACETRACK_WIDTH, RACETRACK_WIDTH, 2, CAMERA_HEIGHT - 2 * RACETRACK_WIDTH, vertexBufferObjectManager);
        final Rectangle rightInner = new Rectangle(CAMERA_WIDTH - 2 - RACETRACK_WIDTH, RACETRACK_WIDTH, 2, CAMERA_HEIGHT - 2 * RACETRACK_WIDTH, vertexBufferObjectManager);

        final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, bottomOuter, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, topOuter, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, leftOuter, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, rightOuter, BodyType.StaticBody, wallFixtureDef);

        PhysicsFactory.createBoxBody(this.mPhysicsWorld, bottomInner, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, topInner, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, leftInner, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(this.mPhysicsWorld, rightInner, BodyType.StaticBody, wallFixtureDef);

        this.mScene.attachChild(bottomOuter);
        this.mScene.attachChild(topOuter);
        this.mScene.attachChild(leftOuter);
        this.mScene.attachChild(rightOuter);

        this.mScene.attachChild(bottomInner);
        this.mScene.attachChild(topInner);
        this.mScene.attachChild(leftInner);
        this.mScene.attachChild(rightInner);
    }

    /**
     * Turn on or off Hotspot.
     *
     * @param context
     * @param isTurnToOn
     */
    public static void turnOnOffHotspot(Context context, boolean isTurnToOn) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        WifiApControl apControl = WifiApControl.getApControl(wifiManager);
        if (apControl != null) {

            // TURN OFF YOUR WIFI BEFORE ENABLE HOTSPOT
            if (wifiManager.isWifiEnabled()) {
              turnOnOffWifi(context, false);
            }

            apControl.setWifiApEnabled(apControl.getWifiApConfiguration(),
                    isTurnToOn);
        }
    }

    /**
     * Turn On or Off wifi
     *
     * @param context
     * @param isTurnToOn
     */
    public static void turnOnOffWifi(Context context, boolean isTurnToOn) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);

        wifiManager.setWifiEnabled(isTurnToOn);
    }

    private ContactListener createContactListener()
    {
        ContactListener contactListener = new ContactListener()
        {
            @Override
            public void beginContact(Contact contact)
            {
                final Fixture x1 = contact.getFixtureA();
                final Fixture x2 = contact.getFixtureB();
                double score=0;
                try {
                if(x1.getBody().getUserData()=="bax"||x2.getBody().getUserData()=="bax"){
                    Debug.d("BAX COLLISION", x1.getBody().getUserData() + " " + x2.getBody().getUserData());
                        if(x1.getBody().getUserData()=="bax"&&((UserDataCar)(x2.getBody().getUserData())).getIdentificador()=="car") {
                            score = listaScore.get(((UserDataCar) (x2.getBody().getUserData())).getTag()) - 1;
                            listaScore.remove(((UserDataCar) (x2.getBody().getUserData())).getTag());
                            listaScore.put(((UserDataCar) (x2.getBody().getUserData())).getTag(), score);
                            punch.play();
                            Debug.d("SCORE", String.valueOf(score));
                        }
                }else if(((UserDataCar)(x1.getBody().getUserData())).getIdentificador()=="car"&&((UserDataCar)(x2.getBody().getUserData())).getIdentificador()=="car") {

                    score = listaScore.get(((UserDataCar)(x1.getBody().getUserData())).getTag()) - 2;
                    listaScore.remove(((UserDataCar)(x1.getBody().getUserData())).getTag());
                    score = listaScore.get(((UserDataCar)(x2.getBody().getUserData())).getTag()) - 2;
                    listaScore.remove(((UserDataCar)(x2.getBody().getUserData())).getTag());
                    listaScore.put(((UserDataCar) (x1.getBody().getUserData())).getTag(), score);
                    listaScore.put(((UserDataCar)(x2.getBody().getUserData())).getTag(), score);
                    punch.play();
                    Debug.d("COLLISION", ((UserDataCar) (x1.getBody().getUserData())).getIdentificador() + " " + ((UserDataCar) (x2.getBody().getUserData())).getIdentificador());
                }
                }catch(NullPointerException npe){
                    Debug.e("NullPointerException");
                }
                catch(NumberFormatException nfe){
                    Debug.e("NumberFormatException");
                }
                catch(ClassCastException cce){
                    Debug.e("ClassCastException");
                }

            }

            @Override
            public void endContact(Contact contact)
            {

            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold)
            {

            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse)
            {

            }
        };
        return contactListener;
    }



    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    TextView info, infoip, msg;
    String message = "";
    ServerSocket serverSocket;

    // Media player



    private JSONArray jsonArreglo = new JSONArray();


    private JSONObject json;

    private List<String> listaIPS = new ArrayList<String>();
    private boolean enviarBloqueados = false;

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "IP: " + inetAddress.getHostAddress() + "\n";
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            //ip += "Algo paso! " + e.toString() + "\n";
        }
        return ip;
    }



    private class SocketServerThread extends Thread {
        String accion="";
        double velocidadx=0;
        double velocidady=0;
        double rotacion = 0;
        int tag=-1;

        static final int SocketServerPORT = 3389;
        int count = 0;

        @Override
        public void run() {
            Socket socket = null;
           DataInputStream dataInputStream = null;
            //DataOutputStream dataOutputStream = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);


                RacerGameActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });

                while (true) {
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(
                            socket.getInputStream());
                    final DataOutputStream  dataOutputStream = new DataOutputStream(
                            socket.getOutputStream());
                    String messageFromClient = "";

                    // If no message sent from client, this code will block the
                    // program
                    messageFromClient = dataInputStream.readUTF();


                    count++;
                    message = messageFromClient;

                    try {
                        json = new JSONObject(message);
                        accion=json.getString("accion");
                    } catch (JSONException e1) {
                        // TODO Auto-generated catch block
                        //e1.printStackTrace();
                    }
                    RacerGameActivity.this.runOnUiThread(new Runnable() {
                        TiledSprite tempCar=null;
                        double tempScore=0;
                        @Override
                        public void run() {
                            Log.v("algo",json.toString());
                            if (accion.equals("movimiento")) {

                                try {
                                    velocidadx = json.getDouble("velocidadx");
                                    velocidady = json.getDouble("velocidady");
                                    rotacion = json.getDouble("rotacion");
                                    tag = json.getInt("tag");


                                    if(mScene.getChildByTag(tag)==null) {
                                        indexCar = json.getInt("indexCar");
                                        initCar(tag);
                                        tempScore = json.getDouble("score");
                                        listaScore.put(tag, tempScore);
                                    }
                                    tempScore = json.getInt("score");
                                    tempCar=(TiledSprite) mScene.getChildByTag(tag);

                                } catch (JSONException e) {
                                    //e.printStackTrace();
                                }

                                final Body carBody = listaCarritos.get(tag);

                                final Vector2 velocity = new Vector2((float)velocidadx,(float)velocidady);
                                try {
                                    carBody.setLinearVelocity(velocity);
                                    //Vector2Pool.recycle(velocity);

                                    carBody.setTransform(carBody.getWorldCenter(), (float) rotacion);

                                    tempCar.setRotation(MathUtils.radToDeg((float) rotacion));

                                    if (tempScore != listaScore.get(tag)) {
                                        if (listaScore.get(tag) <= 0) {
                                            mScene.detachChild(tag);
                                        }
                                        JSONObject jsonScore = new JSONObject();
                                        try {
                                            jsonScore.put("accion", "uScore");
                                            jsonScore.put("Score", listaScore.get(tag));
                                            dataOutputStream.writeUTF(jsonScore.toString());
                                        } catch (JSONException e) {
                                            //e.printStackTrace();
                                        } catch (IOException ioe) {
                                        }

                                    /*if (dataOutputStream != null) {
                                        try {
                                            dataOutputStream.close();
                                        } catch (IOException e) {
                                            // TODO Auto-generated catch block
                                            //e.printStackTrace();
                                        }
                                    }*/
                                    }
                                }catch(NullPointerException npe){
                                    Toast.makeText(getApplicationContext(), "Error, reiniciar el juego por favor", Toast.LENGTH_LONG);
                                }

                            }else if(accion.equals("desconexion")){
                                try {
                                    tag = json.getInt("tag");

                                   mScene.detachChild(tag);

                                } catch (JSONException e) {
                                    //e.printStackTrace();
                                }
                            }

                        }
                    });
                    JSONObject mainObj = new JSONObject();
                    try {
                        mainObj.put("accion", "");
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                       //e.printStackTrace();
                    }
                    dataOutputStream.writeUTF(mainObj.toString());

                    if (dataOutputStream != null) {
                        try {
                            dataOutputStream.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            //e.printStackTrace();
                        }
                    }
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
                RacerGameActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                    }
                });

            } finally {
                if (socket != null) {
                    try {
                        socket.close();

                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        //e.printStackTrace();
                    }
                }

                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        //e.printStackTrace();
                    }
                }

                /*if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        //e.printStackTrace();
                    }
                }*/
            }
        }
    }

    class UserDataCar{
        private int tag;
        private String identificador;

        public UserDataCar(int tag, String identificador){
            this.tag = tag;
            this.identificador = identificador;
        }

        public int getTag(){
            return this.tag;
        }

        public String getIdentificador(){
            return this.identificador;
        }
    }

}