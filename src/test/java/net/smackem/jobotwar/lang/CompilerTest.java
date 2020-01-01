package net.smackem.jobotwar.lang;

import org.junit.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

public class CompilerTest {

    @Test
    public void testWriteRegisters() {
        final String source = "" +
                "1 -> AIM\n" +
                "2 -> RADAR\n" +
                "3 -> SPEEDX\n" +
                "4 -> SPEEDY\n" +
                "5 -> SHOT\n";

        final Compiler compiler = new Compiler();
        final Program program = compiler.compile(source);
        final TestEnvironment env = new TestEnvironment();
        final Interpreter interpreter = new Interpreter(program, env);
        runComplete(interpreter);

        assertThat(env.readAim()).isEqualTo(1);
        assertThat(env.readRadar()).isEqualTo(2);
        assertThat(env.readSpeedX()).isEqualTo(3);
        assertThat(env.readSpeedY()).isEqualTo(4);
        assertThat(env.readShot()).isEqualTo(5);
    }

    @Test
    public void testReadRegisters() throws Exception {
        final String source = "" +
                "AIM -> SHOT\n" +
                "RADAR -> SHOT\n" +
                "SPEEDX -> SHOT\n" +
                "SPEEDY -> SHOT\n" +
                "X -> SHOT\n" +
                "Y -> SHOT\n" +
                "DAMAGE -> SHOT\n";

        final Compiler compiler = new Compiler();
        final Program program = compiler.compile(source);
        final TestEnvironment env = new TestEnvironment();
        final Interpreter interpreter = new Interpreter(program, env);

        env.writeAim(1);
        env.writeRadar(2);
        env.writeSpeedX(3);
        env.writeSpeedY(4);
        env.setX(6);
        env.setY(7);
        env.setDamage(8);

        assertThat(interpreter.runToNextLabel()).isTrue();
        assertThat(env.readShot()).isEqualTo(1);
        assertThat(interpreter.runToNextLabel()).isTrue();
        assertThat(env.readShot()).isEqualTo(2);
        assertThat(interpreter.runToNextLabel()).isTrue();
        assertThat(env.readShot()).isEqualTo(3);
        assertThat(interpreter.runToNextLabel()).isTrue();
        assertThat(env.readShot()).isEqualTo(4);
        assertThat(interpreter.runToNextLabel()).isTrue();
        assertThat(env.readShot()).isEqualTo(6);
        assertThat(interpreter.runToNextLabel()).isTrue();
        assertThat(env.readShot()).isEqualTo(7);
        assertThat(interpreter.runToNextLabel()).isTrue();
        assertThat(env.readShot()).isEqualTo(8);
        assertThat(interpreter.runToNextLabel()).isFalse();
    }

    @Test
    public void testDef() {
        final String source = "" +
                "def x\n" +
                "def y\n" +
                "1.5 -> x\n" +
                "2.5 -> y\n" +
                "x + y -> SHOT\n";

        final Compiler compiler = new Compiler();
        final Program program = compiler.compile(source);
        final TestEnvironment env = new TestEnvironment();
        final Interpreter interpreter = new Interpreter(program, env);

        runComplete(interpreter);

        assertThat(env.readShot()).isEqualTo(4);
    }

    @Test
    public void testLoop() {
        final String source = "" +
                "def i\n" +
                "Loop:\n" +
                "   i + 1 -> i\n" +
                "   goto Loop if i < 100\n" +
                "i -> SHOT\n";

        final Compiler compiler = new Compiler();
        final Program program = compiler.compile(source);
        final TestEnvironment env = new TestEnvironment();
        final Interpreter interpreter = new Interpreter(program, env);

        System.out.println(program.instructions().stream()
                .map(Instruction::toString)
                .collect(Collectors.joining("\n")));

        runComplete(interpreter);

        assertThat(env.readShot()).isEqualTo(100);
    }

    @Test
    public void testUnless() {
        final String source = "" +
                "def i\n" +
                "1 -> SHOT unless i = 1\n" +
                "2 -> AIM unless i = 0\n";

        final Compiler compiler = new Compiler();
        final Program program = compiler.compile(source);
        final TestEnvironment env = new TestEnvironment();
        final Interpreter interpreter = new Interpreter(program, env);

        runComplete(interpreter);

        assertThat(env.readShot()).isEqualTo(1);
        assertThat(env.readAim()).isNotEqualTo(2);
    }

    @Test
    public void testComplex() {
        final String source = "" +
                "def x\n" +
                "def y\n" +
                "def targetX\n" +
                "def targetY\n" +
                "100 -> targetX\n" +
                "60 -> targetY\n" +
                "MoveX:\n" +
                "   x + 1 -> x\n" +
                "   goto MoveX unless x >= targetX\n" +
                "MoveY:\n" +
                "   y + 1 -> y\n" +
                "   goto MoveY unless y >= targetY\n" +
                "Scan:\n" +
                "   RADAR + 5 -> RADAR\n" +
                "   goto Scan if RADAR < 360\n" +
                "RADAR -> AIM\n" +
                "200 + RADAR + AIM + x + y -> SHOT\n" +
                "targetX + 50 -> targetX\n" +
                "targetY + 10 -> targetY\n" +
                "goto MoveX unless targetX > 1000\n" +
                "x -> SPEEDX\n" +
                "y -> SPEEDY\n";

        final Compiler compiler = new Compiler();
        final Program program = compiler.compile(source);
        final TestEnvironment env = new TestEnvironment();
        final Interpreter interpreter = new Interpreter(program, env);

        runComplete(interpreter);

        assertThat(env.readSpeedX()).isGreaterThanOrEqualTo(1000);
        assertThat(env.readShot()).isEqualTo(env.readSpeedX() + env.readSpeedY() + 200 + env.readRadar() + env.readAim());
    }

    @Test
    public void testArithmetics() {
        final String source = "" +
                "1 + 5 / 2 -> SHOT\n" +
                "2 * 5 - 1 -> SPEEDX\n" +
                "1 + 5 * 3 -> AIM\n";

        final Compiler compiler = new Compiler();
        final Program program = compiler.compile(source);
        final TestEnvironment env = new TestEnvironment();
        final Interpreter interpreter = new Interpreter(program, env);

        runComplete(interpreter);

        assertThat(env.readShot()).isEqualTo(3.5);
        assertThat(env.readSpeedX()).isEqualTo(9);
        assertThat(env.readAim()).isEqualTo(16);
    }

    private void runComplete(Interpreter interpreter) {
        try {
            while (interpreter.runToNextLabel()) {
                // proceed until program has finished
            }
        } catch (Interpreter.StackException e) {
            assertThat(true).isFalse();
        }
    }

    private static class TestEnvironment implements RuntimeEnvironment {
        private double aim;
        private double radar;
        private double speedX;
        private double speedY;
        private double x;
        private double y;
        private double shot;
        private double damage;

        @Override
        public double readAim() {
            return this.aim;
        }

        @Override
        public void writeAim(double value) {
            this.aim = value;
        }

        @Override
        public double readRadar() {
            return this.radar;
        }

        @Override
        public void writeRadar(double value) {
            this.radar = value;
        }

        @Override
        public double readSpeedX() {
            return this.speedX;
        }

        @Override
        public void writeSpeedX(double value) {
            this.speedX = value;
        }

        @Override
        public double readSpeedY() {
            return this.speedY;
        }

        @Override
        public void writeSpeedY(double value) {
            this.speedY = value;
        }

        @Override
        public double readX() {
            return this.x;
        }

        public void setX(double value) {
            this.x = value;
        }

        @Override
        public double readY() {
            return this.y;
        }

        public void setY(double value) {
            this.y = value;
        }

        @Override
        public double readDamage() {
            return this.damage;
        }

        public void setDamage(double value) {
            this.damage = value;
        }

        @Override
        public double readShot() {
            return this.shot;
        }

        @Override
        public void writeShot(double value) {
            this.shot = value;
        }

        @Override
        public double getRandom() {
            return 42;
        }
    }
}