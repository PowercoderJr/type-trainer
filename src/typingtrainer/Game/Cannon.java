package typingtrainer.Game;

import javafx.geometry.Point2D;
import javafx.scene.image.WritableImage;

/**
 * Created by Meow on 22.04.2017.
 */
public abstract class Cannon extends PvpObject
{
	public static final double CANNON_KNOCKBACK_STEP_DISTANCE = 20.0;
	public static final int CANNON_KNOCKBACK_STEP_DURATION = 50;

	private Ship parentShip;
	private Point2D basePosition;
	private Thread pushingThread;

	public Cannon(Ship parentShip, Belonging belonging, Point2D position)
	{
		super(belonging, position);
		basePosition = new Point2D(position.getX(), position.getY());
		this.parentShip = parentShip;
		image = new WritableImage(Game.SPRITE_SHEET.getPixelReader(), 132, 0, 150, 55);
	}

	public Cannonball shoot(Point2D target)
	{
		rotationAngle = Math.toDegrees(Math.atan((target.getY() - position.getY() - pivot.getY()) / (target.getX() - position.getX() - pivot.getX())));

		double pivotToMuzzleLength = image.getWidth() - pivot.getX();
		Animation smokeCloud = new Animation(belonging, position.add(image.getWidth() - 8, pivot.getY() - 30), Game.SPRITE_SHEET, 7, 2, 132, 71, 120, 60);
		smokeCloud.setPivot(new Point2D(-pivotToMuzzleLength, smokeCloud.getHeight() / 2));
		smokeCloud.setRotationAngle(rotationAngle);
		getParentShip().getParentGame().getSmokeClouds().add(smokeCloud);
		smokeCloud.play(500);

		Cannonball cannonball = new Cannonball(this, belonging, position.add(pivot));
		cannonball.setTarget(target);
		synchronized (Game.CANNONBALLS_LOCK)
		{
			getParentShip().getParentGame().getCannonballs().add(cannonball); //Перенести ли в методы, откуда вызывается данный?
		}
		//Debug
		//GameSceneController.lines.add(new GameSceneController.Line(belonging == PvpObject.Belonging.HOSTILE ? GameSceneController.DEFAULT_SCREEN_WIDTH - cannonball.getPosition().getX() : cannonball.getPosition().getX(), cannonball.getPosition().getY(), belonging == PvpObject.Belonging.HOSTILE ? GameSceneController.DEFAULT_SCREEN_WIDTH - target.getX() : target.getX(), target.getY()));
		//

		if (pushingThread != null && pushingThread.isAlive())
			pushingThread.interrupt();
		pushingThread = new Thread(() ->
		{
			try
			{
				//Определяется направление отдачи
				double cathetA = pivot.getX();
				double cathetB = cathetA * Math.tan(Math.toRadians(rotationAngle));
				Point2D pointA = new Point2D(position.getX(), position.getY() + pivot.getY() - cathetB);
				Point2D dir = pivot.add(position).subtract(pointA).normalize();
				for (int i = 0; i < 3; ++i)
				{
					position = position.subtract(dir.getX() * CANNON_KNOCKBACK_STEP_DISTANCE, dir.getY() * CANNON_KNOCKBACK_STEP_DISTANCE);
					Thread.sleep(CANNON_KNOCKBACK_STEP_DURATION);
				}

				double step = position.distance(basePosition) / 12;
				dir = basePosition.subtract(position).normalize();
				for (int i = 0; i < 12; ++i)
				{
					position = position.add(dir.getX() * step, dir.getY() * step);
					Thread.sleep(CANNON_KNOCKBACK_STEP_DURATION);
				}
			}
			catch (InterruptedException e)
			{
				//System.out.println(e.getMessage());
			}
		});
		pushingThread.start();
		return cannonball;
	}

	public Ship getParentShip()
	{
		return parentShip;
	}
}
