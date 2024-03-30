import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class JAGE extends JFrame {
    private String title = "Untitled";
    private int width = 800;
    private int height = 600;
    private final JPanel engineWorld;
    private Color engineWorldBackground = new Color(64,64,64);
    private boolean debugEnabled = true;
    private boolean gravityVisable = false;
    private boolean vectorVisable = false;
    private boolean debugPauseEnabled = false;
    private Thread debugPauseFontResizeThread;
    private double debugPauseFontSize = 20;
    private Thread physicsThread;
    private int debugOptionCount=0;
    private int debugOptionPrintCount=0;

    double xGravity = 0;
    double yGravity = 0;
    int engineUpdateSpeedMS = 1;
    ArrayList<GravityPoint> gravityPoints = new ArrayList<>();
    ArrayList<Circle> circles = new ArrayList<>();
    ArrayList<Rectangle> rectangles = new ArrayList<>();

    private void makeDebugPauseFontResizeThread(){
        if(debugPauseFontResizeThread != null){
            debugPauseFontResizeThread.interrupt();
        }
        debugPauseFontResizeThread = new Thread(){
            public void run() {
                super.run();
                double sequence = 0;

                while(debugPauseEnabled){
                    try {
                        //noinspection BusyWait
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if(sequence>=2*Math.PI){
                        sequence -= 2*Math.PI;
                    }
                    debugPauseFontSize = 30 + (Math.sin(sequence) * 10);
                    sequence += (2*Math.PI)/1000;
                }
            }
        };
    }
    private void makePhysicsThread(){
        if(physicsThread != null){
            physicsThread.interrupt();
        }
        physicsThread = new Thread(){
            public void run() {
                super.run();
                while(!debugPauseEnabled){
                    try {
                        //noinspection BusyWait
                        Thread.sleep(engineUpdateSpeedMS);
                    } catch (InterruptedException e) {
                        break;
                    }
                    for(Circle circle : circles){
                        if(circle.staticEnabled) {
                            continue;
                        }
                        if(circle.gravityEnabled){
                            double currentPosXGravity = xGravity;
                            double currentPosYGravity = yGravity;
                            for(GravityPoint gravityPoint:gravityPoints){
                                final double distance = Math.sqrt(Math.pow((gravityPoint.x - circle.x)/100, 2) + Math.pow((gravityPoint.y - circle.y)/100, 2));
                                if(Math.abs(gravityPoint.x - circle.x) < 1) {
                                    final int currentPosXGravitySign = (int) Math.signum(gravityPoint.x - circle.x);
                                    currentPosXGravity += currentPosXGravitySign * gravityPoint.g;
                                }
                                else{
                                    final int currentPosXGravitySign = (int) Math.signum(gravityPoint.x - circle.x);
                                    currentPosXGravity = currentPosXGravitySign * gravityPoint.g / Math.pow(distance, 2) + currentPosXGravity;
                                }
                                if(Math.abs(gravityPoint.y - circle.y) < 1){
                                    final int currentPosYGravitySign = (int) Math.signum(gravityPoint.y - circle.y);
                                    currentPosYGravity += currentPosYGravitySign * gravityPoint.g;
                                }
                                else{
                                    final int currentPosYGravitySign = (int) Math.signum(gravityPoint.y - circle.y);
                                    currentPosYGravity = currentPosYGravitySign * gravityPoint.g / Math.pow(distance, 2) + currentPosYGravity;
                                }
                            }
                            final double currentPosGravityTotal = Math.sqrt(Math.pow(currentPosXGravity, 2) + Math.pow(currentPosYGravity, 2));
                            double currentPosGravityAngle = Math.atan2(currentPosYGravity, currentPosXGravity);
                            double currentPosGravityAngleX = Math.cos(currentPosGravityAngle);
                            double currentPosGravityAngleY = Math.sin(currentPosGravityAngle);
                            circle.xVec += currentPosGravityAngleX*currentPosGravityTotal;
                            circle.yVec += currentPosGravityAngleY*currentPosGravityTotal;
                        }
                        circle.x += circle.xVec;
                        circle.y += circle.yVec;
                        circle.rotate += circle.rotateSpeed;

                        if(circle.borderCollisionEnabled){
                            double frictionConstant = 1;
                            if(circle.frictionEnabled){
                                frictionConstant = 1-circle.friction;
                            }
                            if(circle.x < circle.radius){
                                circle.x = circle.radius;
                                circle.xVec = Math.abs(circle.xVec)*circle.reflectivity;
                                circle.yVec *= frictionConstant;
                            }
                            else if(circle.x > engineWorld.getWidth() - circle.radius){
                                circle.x = engineWorld.getWidth() - circle.radius;
                                circle.xVec = -Math.abs(circle.xVec)*circle.reflectivity;
                                circle.yVec *= frictionConstant;
                            }
                            if(circle.y < circle.radius){
                                circle.y = circle.radius;
                                circle.yVec = Math.abs(circle.yVec)*circle.reflectivity;
                                circle.xVec *= frictionConstant;
                            }
                            if(circle.y > engineWorld.getHeight() - circle.radius){
                                circle.y = engineWorld.getHeight() - circle.radius;
                                circle.yVec = -Math.abs(circle.yVec)*circle.reflectivity;
                                circle.xVec *= frictionConstant;
                            }
                        }
                        if(circle.collisionEnabled){
                            for(Circle targetCircle:circles){
                                if(targetCircle == circle){
                                    continue;
                                }
                                if(circle.isColliding(targetCircle)) {
                                    final double distance = Math.sqrt(Math.pow((targetCircle.x - circle.x), 2) + Math.pow((targetCircle.y - circle.y), 2));
                                    final double overlap = circle.radius + targetCircle.radius - distance;
                                    final double angle = Math.atan2(targetCircle.y - circle.y, targetCircle.x - circle.x);
                                    final double targetCircleXVec = Math.cos(angle) * overlap / 2;
                                    final double targetCircleYVec = Math.sin(angle) * overlap / 2;
                                    final double circleXVec = -Math.cos(angle) * overlap / 2;
                                    final double circleYVec = -Math.sin(angle) * overlap / 2;
                                    targetCircle.xVec += targetCircleXVec;
                                    targetCircle.yVec += targetCircleYVec;
                                    circle.xVec += circleXVec;
                                    circle.yVec += circleYVec;


                                }
                            }
                        }
                    }
                    for(Rectangle rectangle : rectangles){
                        if(rectangle.staticEnabled){
                            continue;
                        }

                        rectangle.x+= rectangle.xVec;
                        rectangle.y+= rectangle.yVec;
                        rectangle.rotate += rectangle.rotateSpeed;

                        if(rectangle.borderCollisionEnabled){
                            double frictionConstant = 1;
                            if(rectangle.frictionEnabled){
                                frictionConstant = 1-rectangle.friction;
                            }
                            if(rectangle.x < rectangle.getWidthWithRotate()/2){
                                rectangle.x = rectangle.getWidthWithRotate()/2;
                                rectangle.xVec = Math.abs(rectangle.xVec)*rectangle.reflectivity;
                                rectangle.yVec *= frictionConstant;

                                double rotateCopy = rectangle.rotate;
                                while(rotateCopy >= Math.PI/4){
                                    rotateCopy -= Math.PI/2;
                                }
                                while(rotateCopy <= -Math.PI/4){
                                    rotateCopy += Math.PI/2;
                                }
                                if(rotateCopy > 0){
                                    rectangle.rotateSpeed = -rectangle.rotateSpeed;
                                }
                            }
                            else if(rectangle.x > engineWorld.getWidth() - rectangle.getWidthWithRotate()/2){
                                rectangle.x = engineWorld.getWidth() - rectangle.getWidthWithRotate()/2;
                                rectangle.xVec = -Math.abs(rectangle.xVec)*rectangle.reflectivity;
                                rectangle.yVec *= frictionConstant;

                                double rotateCopy = rectangle.rotate;
                                while(rotateCopy >= Math.PI/4){
                                    rotateCopy -= Math.PI/2;
                                }
                                while(rotateCopy <= -Math.PI/4){
                                    rotateCopy += Math.PI/2;
                                }
                                if(rotateCopy < 0){
                                    rectangle.rotateSpeed = -rectangle.rotateSpeed;
                                }
                            }
                            if(rectangle.y < rectangle.getHeightWithRotate()/2){
                                rectangle.y = rectangle.getHeightWithRotate()/2;
                                rectangle.yVec = Math.abs(rectangle.yVec)*rectangle.reflectivity;
                                rectangle.xVec *= frictionConstant;

                                double rotateCopy = rectangle.rotate;
                                while (rotateCopy >= Math.PI/4){
                                    rotateCopy -= Math.PI/2;
                                }
                                while (rotateCopy <= -Math.PI/4){
                                    rotateCopy += Math.PI/2;
                                }
                                if(rotateCopy > 0){
                                    rectangle.rotateSpeed = -rectangle.rotateSpeed;
                                }

                            }
                            else if(rectangle.y > engineWorld.getHeight() - rectangle.getHeightWithRotate()/2){
                                rectangle.y = engineWorld.getHeight() - rectangle.getHeightWithRotate()/2;
                                rectangle.yVec = -Math.abs(rectangle.yVec)*rectangle.reflectivity;
                                rectangle.xVec *= frictionConstant;

                                double rotateCopy = rectangle.rotate;
                                while (rotateCopy >= Math.PI/4){
                                    rotateCopy -= Math.PI/2;
                                }
                                while (rotateCopy <= -Math.PI/4){
                                    rotateCopy += Math.PI/2;
                                }
                                if(rotateCopy < 0){
                                    rectangle.rotateSpeed = -rectangle.rotateSpeed;
                                }
                            }
                        }
                        if(rectangle.collisionEnabled){
                            for(Circle circle : circles){
                                if(rectangle.x - rectangle.getWidthWithRotate()/2 < circle.x + circle.radius &&
                                        rectangle.x + rectangle.getWidthWithRotate()/2 > circle.x - circle.radius &&
                                        rectangle.y - rectangle.getHeightWithRotate()/2 < circle.y + circle.radius &&
                                        rectangle.y + rectangle.getHeightWithRotate()/2 > circle.y - circle.radius){
                                    final double distance = Math.sqrt(Math.pow((circle.x - rectangle.x), 2) + Math.pow((circle.y - rectangle.y), 2));
                                    final double overlap = circle.radius + Math.sqrt(Math.pow(rectangle.getWidthWithRotate()/2, 2) + Math.pow(rectangle.getHeightWithRotate()/2, 2)) - distance;
                                    final double angle = Math.atan2(circle.y - rectangle.y, circle.x - rectangle.x);
                                    final double circleXVec = Math.cos(angle) * overlap / 2;
                                    final double circleYVec = Math.sin(angle) * overlap / 2;
                                    final double rectangleXVec = -Math.cos(angle) * overlap / 2;
                                    final double rectangleYVec = -Math.sin(angle) * overlap / 2;
                                    circle.xVec += circleXVec;
                                    circle.yVec += circleYVec;
                                    rectangle.xVec += rectangleXVec;
                                    rectangle.yVec += rectangleYVec;
                                }
                            }
                            for(Rectangle compRectangle : rectangles){
                                if(compRectangle == rectangle){
                                    continue;
                                }
                                if(rectangle.x - rectangle.getWidthWithRotate()/2 < compRectangle.x + compRectangle.getWidthWithRotate()/2 &&
                                        rectangle.x + rectangle.getWidthWithRotate()/2 > compRectangle.x - compRectangle.getWidthWithRotate()/2 &&
                                        rectangle.y - rectangle.getHeightWithRotate()/2 < compRectangle.y + compRectangle.getHeightWithRotate()/2 &&
                                        rectangle.y + rectangle.getHeightWithRotate()/2 > compRectangle.y - compRectangle.getHeightWithRotate()/2){
                                    final double distance = Math.sqrt(Math.pow((compRectangle.x - rectangle.x), 2) + Math.pow((compRectangle.y - rectangle.y), 2));
                                    final double overlap = Math.sqrt(Math.pow((rectangle.getWidthWithRotate() + compRectangle.getWidthWithRotate())/2, 2) + Math.pow((rectangle.getHeightWithRotate() + compRectangle.getHeightWithRotate())/2, 2)) - distance;
                                    final double angle = Math.atan2(compRectangle.y - rectangle.y, compRectangle.x - rectangle.x);
                                    final double compRectangleXVec = Math.cos(angle) * overlap / 2;
                                    final double compRectangleYVec = Math.sin(angle) * overlap / 2;
                                    final double rectangleXVec = -Math.cos(angle) * overlap / 2;
                                    final double rectangleYVec = -Math.sin(angle) * overlap / 2;
                                    compRectangle.xVec += compRectangleXVec;
                                    compRectangle.yVec += compRectangleYVec;
                                    rectangle.xVec += rectangleXVec;
                                    rectangle.yVec += rectangleYVec;
                                }
                            }
                        }
                    }
                }
            }
        };
        physicsThread.start();
    }


    public JAGE() {
        super();
        setTitle(title);
        setSize(width, height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        engineWorld = new JPanel(null){
            public void paintComponent(Graphics simpleGraphics) {
                super.paintComponent(simpleGraphics);
                Graphics2D graphics = (Graphics2D) simpleGraphics;
                AffineTransform graphicsOrigin = graphics.getTransform();

                for(GravityPoint gravityPoint : gravityPoints){
                    if(gravityPoint.visible){
                        graphics.setColor(gravityPoint.color);
                        graphics.fillOval((int)(gravityPoint.x - gravityPoint.radius), (int)(gravityPoint.y - gravityPoint.radius), (int)(gravityPoint.radius*2), (int)(gravityPoint.radius*2));
                    }
                }
                for(Circle circle : circles){
                    if(circle.visible){
                        graphics.setTransform(graphicsOrigin);
                        graphics.setColor(circle.color);
                        graphics.rotate(circle.rotate, (int)(circle.x), (int)(circle.y));
                        graphics.fillOval((int)(circle.x - circle.radius), (int)(circle.y - circle.radius), (int)(circle.radius*2), (int)(circle.radius*2));
                    }

                }
                for(Rectangle rectangle : rectangles){
                    if(rectangle.visible){
                        graphics.setTransform(graphicsOrigin);
                        graphics.setColor(rectangle.color);
                        graphics.rotate(rectangle.rotate, (int)(rectangle.x), (int)(rectangle.y));
                        graphics.fillRect((int)(rectangle.x - rectangle.xSize/2), (int)(rectangle.y - rectangle.ySize/2), (int)(rectangle.xSize), (int)(rectangle.ySize));
                    }
                }
                graphics.setTransform(graphicsOrigin);
                if(debugEnabled){
                    graphics.setColor(Color.red);
                    graphics.setFont(new Font("", Font.PLAIN, 12));
                    graphics.drawString("Debug Enabled", 0, 12);
                    if(debugPauseEnabled){
                        graphics.setColor(Color.red);
                        graphics.drawString("Debug Paused On", 0, getHeight() -10);
                        graphics.setFont(new Font("", Font.PLAIN, (int)debugPauseFontSize));
                        graphics.drawString("Debug Paused", getWidth() - graphics.getFontMetrics().stringWidth("Debug Paused"), getHeight() - 100 + (int)debugPauseFontSize);
                    }
                    if(gravityVisable){
                        graphics.setColor(Color.red);
                        graphics.setFont(new Font("", Font.PLAIN, 12));
                        graphics.drawString("Gravity Visable On", 0, getHeight() - 20);
                        graphics.setColor(Color.yellow);


                        final int arrowXCount = getWidth()/20;
                        final int arrowYCount = getHeight()/20;
                        for(int i=0; i<arrowXCount; i++){
                            for(int j=0; j<arrowYCount; j++){
                                double currentPosXGravity = xGravity;
                                double currentPosYGravity = yGravity;
                                for(GravityPoint gravityPoint:gravityPoints){
                                    final double distance = Math.sqrt(Math.pow((gravityPoint.x - i*20)/100, 2) + Math.pow((gravityPoint.y - j*20)/100, 2));
                                    if(Math.abs(gravityPoint.x - i*20) < 1) {
                                        final int currentPosXGravitySign = (int) Math.signum(gravityPoint.x - i*20);
                                        currentPosXGravity += currentPosXGravitySign * gravityPoint.g;
                                    }
                                    else{
                                        final int currentPosXGravitySign = (int) Math.signum(gravityPoint.x - i*20);
                                        currentPosXGravity = currentPosXGravitySign * gravityPoint.g * 10 / Math.pow(distance, 2) + currentPosXGravity;
                                    }
                                    if(Math.abs(gravityPoint.y - j*20) < 1){
                                        final int currentPosYGravitySign = (int) Math.signum(gravityPoint.y - j*20);
                                        currentPosYGravity += currentPosYGravitySign * gravityPoint.g;
                                    }
                                    else{
                                        final int currentPosYGravitySign = (int) Math.signum(gravityPoint.y - j*20);
                                        currentPosYGravity = currentPosYGravitySign * gravityPoint.g * 10 / Math.pow(distance, 2) + currentPosYGravity;
                                    }
                                }
                                final double currentPosGravityTotal = Math.sqrt(Math.pow(currentPosXGravity, 2) + Math.pow(currentPosYGravity, 2));
                                double currentPosGravityAngle = Math.atan2(currentPosYGravity, currentPosXGravity);
                                double currentPosGravityAngleX = Math.cos(currentPosGravityAngle);
                                double currentPosGravityAngleY = Math.sin(currentPosGravityAngle);

                                graphics.drawLine(i*20, j*20, (int)(i*20 + currentPosGravityAngleX*currentPosGravityTotal*20), (int)(j*20 + currentPosGravityAngleY*currentPosGravityTotal*20));
                                //make arrow like this →
                                graphics.drawLine((int)(i*20 + currentPosGravityAngleX*currentPosGravityTotal*20), (int)(j*20 + currentPosGravityAngleY*currentPosGravityTotal*20), (int)((i*20 + currentPosGravityAngleX*currentPosGravityTotal*20) + Math.cos(currentPosGravityAngle+Math.toRadians(135))*5), (int)((j*20 + currentPosGravityAngleY*currentPosGravityTotal*20) + Math.sin(currentPosGravityAngle+Math.toRadians(135))*5));
                                graphics.drawLine((int)(i*20 + currentPosGravityAngleX*currentPosGravityTotal*20), (int)(j*20 + currentPosGravityAngleY*currentPosGravityTotal*20), (int)((i*20 + currentPosGravityAngleX*currentPosGravityTotal*20) + Math.cos(currentPosGravityAngle-Math.toRadians(135))*5), (int)((j*20 + currentPosGravityAngleY*currentPosGravityTotal*20) + Math.sin(currentPosGravityAngle-Math.toRadians(135))*5));
                            }
                        }
                    }
                    if(vectorVisable){
                        for(Circle circle : circles) {
                            graphics.setColor(Color.green);
                            graphics.setStroke(new BasicStroke(2));
                            graphics.drawLine((int) circle.x, (int) circle.y, (int) (circle.x + circle.xVec * 100), (int) (circle.y + circle.yVec * 100));
                            graphics.drawLine((int) (circle.x + circle.xVec * 100), (int) (circle.y + circle.yVec * 100), (int) ((circle.x + circle.xVec * 100) + Math.cos(Math.atan2(circle.yVec, circle.xVec) + Math.toRadians(135)) * 5), (int) ((circle.y + circle.yVec * 100) + Math.sin(Math.atan2(circle.yVec, circle.xVec) + Math.toRadians(135)) * 5));
                            graphics.drawLine((int) (circle.x + circle.xVec * 100), (int) (circle.y + circle.yVec * 100), (int) ((circle.x + circle.xVec * 100) + Math.cos(Math.atan2(circle.yVec, circle.xVec) - Math.toRadians(135)) * 5), (int) ((circle.y + circle.yVec * 100) + Math.sin(Math.atan2(circle.yVec, circle.xVec) - Math.toRadians(135)) * 5));
                        }
                        for(Rectangle rectangle : rectangles){
                            graphics.setColor(Color.green);
                            graphics.setStroke(new BasicStroke(2));
                            graphics.drawLine((int) rectangle.x, (int) rectangle.y, (int) (rectangle.x + rectangle.xVec * 100), (int) (rectangle.y + rectangle.yVec * 100));
                            graphics.drawLine((int) (rectangle.x + rectangle.xVec * 100), (int) (rectangle.y + rectangle.yVec * 100), (int) ((rectangle.x + rectangle.xVec * 100) + Math.cos(Math.atan2(rectangle.yVec, rectangle.xVec) + Math.toRadians(135)) * 5), (int) ((rectangle.y + rectangle.yVec * 100) + Math.sin(Math.atan2(rectangle.yVec, rectangle.xVec) + Math.toRadians(135)) * 5));
                            graphics.drawLine((int) (rectangle.x + rectangle.xVec * 100), (int) (rectangle.y + rectangle.yVec * 100), (int) ((rectangle.x + rectangle.xVec * 100) + Math.cos(Math.atan2(rectangle.yVec, rectangle.xVec) - Math.toRadians(135)) * 5), (int) ((rectangle.y + rectangle.yVec * 100) + Math.sin(Math.atan2(rectangle.yVec, rectangle.xVec) - Math.toRadians(135)) * 5));
                        }
                        graphics.setTransform(graphicsOrigin);
                        graphics.setColor(Color.red);
                        graphics.setFont(new Font("", Font.PLAIN, 12));
                        graphics.drawString("Vector Visable On", 0, getHeight() - 30);
                    }
                }

                repaint();
            }
        };
        engineWorld.setBackground(engineWorldBackground);
        add(engineWorld);
        KeyAdapter debugPauseKeyAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F3 && debugEnabled) {
                    debugPauseEnabled = !debugPauseEnabled;
                    if (debugPauseEnabled) {
                        debugOptionCount++;
                        makeDebugPauseFontResizeThread();
                        debugPauseFontResizeThread.start();
                    } else {
                        debugOptionCount--;
                        makePhysicsThread();
                    }
                }
            }
        };
        addKeyListener(debugPauseKeyAdapter);
        KeyAdapter gravityVisableKeyAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F4 && debugEnabled) {
                    gravityVisable = !gravityVisable;
                    if(gravityVisable){
                        debugOptionCount++;
                    }
                    else{
                        debugOptionCount--;
                    }
                }
            }
        };
        addKeyListener(gravityVisableKeyAdapter);
        KeyAdapter vectorVisableKeyAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F5 && debugEnabled) {
                    vectorVisable = !vectorVisable;
                    if(vectorVisable){
                        debugOptionCount++;
                    }
                    else{
                        debugOptionCount--;
                    }
                }
            }
        };
        addKeyListener(vectorVisableKeyAdapter);

        makePhysicsThread();

        setVisible(true);
    }

    public void setTitle(String title) {
        this.title = title;
        super.setTitle(title);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        super.setSize(width, height);
    }

    public void setBackgroundColor(Color color) {
        engineWorldBackground = color;
        engineWorld.setBackground(color);
    }

    public void setDebug(boolean debug) {
        debugEnabled = debug;
    }

    class GravityPoint{
        double x;
        double y;
        double g;
        double radius = 5;
        Color color = Color.cyan;
        boolean enabled = true;
        boolean visible = true;
        public GravityPoint(double x, double y, double g){
            this.x = x;
            this.y = y;
            this.g = g;
        }
    }
    class Circle{
        double x;
        double y;
        double xVec=0;
        double yVec=0;
        double radius;
        double rotate = 0;
        double rotateSpeed = 0;
        double mass = 10;
        double reflectivity = 0.8;
        double friction = 0.1;
        Color color = Color.white;
        boolean staticEnabled = false;
        boolean gravityEnabled = true;
        boolean borderCollisionEnabled = true;
        boolean collisionEnabled = true;
        boolean frictionEnabled = false;
        boolean visible = true;
        public Circle(double x, double y, double radius){
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.mass = radius;
        }
        boolean isColliding(Circle circle){
            return Math.sqrt(Math.pow((circle.x - x), 2) + Math.pow((circle.y - y), 2)) < circle.radius + radius;
        }
    }

    class Rectangle{
        double x;
        double y;
        double xSize;
        double ySize;
        double xVec = 0;
        double yVec = 0;
        double mass;
        double reflectivity = 0.8;
        double friction = 0.1;
        double rotate = 0;
        double rotateSpeed = 0;
        Color color = Color.white;
        boolean staticEnabled = false;
        boolean gravityEnabled = true;
        boolean borderCollisionEnabled = true;
        boolean collisionEnabled = true;
        boolean frictionEnabled = false;
        boolean visible = true;

        public Rectangle(double x, double y, double xSize, double ySize){
            this.x = x;
            this.y = y;
            this.xSize = xSize;
            this.ySize = ySize;
            this.mass = xSize*ySize;
        }
        double getWidthWithRotate(){
            return Math.abs(xSize*Math.cos(rotate)) + Math.abs(ySize*Math.sin(rotate));
        }
        double getHeightWithRotate(){
            return Math.abs(xSize*Math.sin(rotate)) + Math.abs(ySize*Math.cos(rotate));
        }

    }
}