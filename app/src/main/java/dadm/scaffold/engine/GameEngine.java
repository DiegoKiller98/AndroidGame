package dadm.scaffold.engine;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dadm.scaffold.ScaffoldActivity;
import dadm.scaffold.counter.GameFragment;
import dadm.scaffold.input.InputController;
import dadm.scaffold.sound.GameEvent;
import dadm.scaffold.sound.SoundManager;

public class GameEngine {


    private List<GameObject> gameObjects = new ArrayList<GameObject>();
    private List<GameObject> objectsToAdd = new ArrayList<GameObject>();
    private List<GameObject> objectsToRemove = new ArrayList<GameObject>();
    private List<Collision> detectedCollisions = new ArrayList<Collision>();
    private QuadTree quadTree = new QuadTree();

    private UpdateThread theUpdateThread;
    private DrawThread theDrawThread;
    public InputController theInputController;
    private final GameView theGameView;

    public Random random = new Random();

    private SoundManager soundManager;

    public int width;
    public int height;
    public double pixelFactor;
    private int scoreToAdd = 0;
    private int livesToTake = 0;

    public Activity mainActivity;

    public GameEngine(Activity activity, GameView gameView) {
        mainActivity = activity;

        theGameView = gameView;
        theGameView.setGameObjects(this.gameObjects);

        QuadTree.init();

        this.width = theGameView.getWidth()
                - theGameView.getPaddingRight() - theGameView.getPaddingLeft();
        this.height = theGameView.getHeight()
                - theGameView.getPaddingTop() - theGameView.getPaddingTop();

        quadTree.setArea(new Rect(0, 0, width, height));

        this.pixelFactor = this.height / 400d;
    }

    public void setTheInputController(InputController inputController) {
        theInputController = inputController;
    }

    public void setViews(ImageView background1, ImageView background2){
        theUpdateThread.backgroundAnimation(background1, background2);
    }

    public void startGame() {
        // Stop a game if it is running
        stopGame();

        // Setup the game objects
        int nugameObjects = gameObjects.size();
        for (int i = 0; i < nugameObjects; i++) {
            gameObjects.get(i).startGame();
        }

        // Start the update thread
        theUpdateThread = new UpdateThread(this);
        theUpdateThread.start();

        // Start the drawing thread
        theDrawThread = new DrawThread(this);
        theDrawThread.start();
    }

    public void stopGame() {
        if (theUpdateThread != null) {
            theUpdateThread.stopGame();
        }
        if (theDrawThread != null) {
            theDrawThread.stopGame();
        }
    }

    public void pauseGame() {
        if (theUpdateThread != null) {
            theUpdateThread.pauseGame();
        }
        if (theDrawThread != null) {
            theDrawThread.pauseGame();
        }
        theInputController.onPause();
    }

    public void resumeGame() {
        if (theUpdateThread != null) {
            theUpdateThread.resumeGame();
        }
        if (theDrawThread != null) {
            theDrawThread.resumeGame();
        }
        theInputController.onResume();
    }

    public void addGameObject(GameObject gameObject) {
        if (isRunning()) {
            objectsToAdd.add(gameObject);
        } else {
            addGameObjectNow(gameObject);
        }
        mainActivity.runOnUiThread(gameObject.onAddedRunnable);
    }

    public void removeGameObject(GameObject gameObject) {
        objectsToRemove.add(gameObject);
        mainActivity.runOnUiThread(gameObject.onRemovedRunnable);
    }

    public void onUpdate(long elapsedMillis) {
        int nugameObjects = gameObjects.size();
        for (int i = 0; i < nugameObjects; i++) {
            GameObject go =  gameObjects.get(i);
            go.onUpdate(elapsedMillis,this);
            if(go instanceof ScreenGameObject) {
                ((ScreenGameObject) go).onPostUpdate(this);
            }
        }
        checkCollisions();
        synchronized (gameObjects) {
            while (!objectsToRemove.isEmpty()) {
                GameObject objectToRemove = objectsToRemove.remove(0);
                gameObjects.remove(objectToRemove);
                if (objectToRemove instanceof  ScreenGameObject) {
                    quadTree.removeGameObject((ScreenGameObject) objectToRemove);
                }
            }
            while (!objectsToAdd.isEmpty()) {
                GameObject gameObject = objectsToAdd.remove(0);
                addGameObjectNow(gameObject);
            }
        }
    }

    public void onDraw() {
        theGameView.draw();

    }

    public boolean isRunning() {
        return theUpdateThread != null && theUpdateThread.isGameRunning();
    }

    public boolean isPaused() {
        return theUpdateThread != null && theUpdateThread.isGamePaused();
    }

    public Context getContext() {
        return theGameView.getContext();
    }

    private void checkCollisions() {
        // Release the collisions from the previous step
        while (!detectedCollisions.isEmpty()) {
            Collision.release(detectedCollisions.remove(0));
        }
        quadTree.checkCollisions(this, detectedCollisions);
    }

    private void addGameObjectNow (GameObject object) {
        gameObjects.add(object);
        if (object instanceof ScreenGameObject) {
            ScreenGameObject sgo = (ScreenGameObject) object;

            quadTree.addGameObject(sgo);
        }
    }

    public void setSoundManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public void onGameEvent (GameEvent gameEvent) {
        // We notify all the GameObjects
        // Also the sound manager
        soundManager.playSoundForGameEvent(gameEvent);
    }

    public void setScoreToAdd(int score) {
        scoreToAdd = score;
    }

    public int getScoreToAdd() {
        return scoreToAdd;
    }

    public void setLivesToTake(int livesToTake) {
        this.livesToTake = livesToTake;
    }

    public int getLivesToTake() {
        return livesToTake;
    }

    public void finalScore(int score){
        ((ScaffoldActivity)mainActivity).finalScore(score);
    }
}
