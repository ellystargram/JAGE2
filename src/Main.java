import java.awt.*;
import java.util.Random;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        JAGE module = new JAGE();
//        module.xGravity = 0.01;
        module.yGravity = 0.03;
//        module.gravityPoints.add(module.new GravityPoint(300,200,0.01));
        module.engineUpdateSpeedMS= 5;
        Random rand = new Random();
        for(int i=0; i<20;i++){
            module.circles.add(module.new Circle(rand.nextDouble(50,600), rand.nextDouble(50,500), rand.nextDouble(5,20)));
            module.circles.get(i).xVec = rand.nextDouble(-1,1);
            module.circles.get(i).yVec = rand.nextDouble(-1,1);
            module.circles.get(i).rotateSpeed = rand.nextDouble(-0.1,0.1);
            module.circles.get(i).color = new Color(rand.nextInt(255),rand.nextInt(255),rand.nextInt(255));
        }
    }
}