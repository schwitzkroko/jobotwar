package net.smackem.jobotwar.lang;

import java.util.*;

public final class Interpreter {
    private final Program program;
    private final List<Instruction> code;
    private final RuntimeEnvironment runtime;
    private final Stack stack = new Stack();
    private int pc;

    public Interpreter(Program program, RuntimeEnvironment runtime) {
        this.program = Objects.requireNonNull(program);
        this.code = new ArrayList<>(program.instructions());
        this.runtime = Objects.requireNonNull(runtime);
    }

    public Program program() {
        return this.program;
    }

    public RuntimeEnvironment runtime() {
        return this.runtime;
    }

    public boolean runToNextLabel() throws StackException {
        while (this.pc < this.code.size()) {
            final int target = executeInstruction(this.code.get(this.pc));
            if (target == -2) {
                this.pc++;
                return true;
            }
            if (target >= 0) {
                this.pc = target;
            } else {
                this.pc++;
            }
        }
        return false;
    }

    private int executeInstruction(Instruction instr) throws StackException {
        double right;
        switch (instr.opCode()) {
            case LD_F64:
                this.stack.push(instr.f64Arg());
                break;
            case LD_REG:
                this.stack.push(loadRegister(instr.strArg()));
                break;
            case LD_LOC:
                this.stack.push(this.stack.get(instr.intArg()));
                break;
            case ST_LOC:
                this.stack.set(instr.intArg(), this.stack.pop());
                break;
            case ST_REG:
                storeRegister(instr.strArg(), this.stack.pop());
                break;
            case ADD:
                this.stack.push(this.stack.pop() + this.stack.pop());
                break;
            case SUB:
                right = this.stack.pop();
                this.stack.push(this.stack.pop() - right);
                break;
            case MUL:
                this.stack.push(this.stack.pop() * this.stack.pop());
                break;
            case DIV:
                right = this.stack.pop();
                this.stack.push(this.stack.pop() / right);
                break;
            case MOD:
                right = this.stack.pop();
                this.stack.push(this.stack.pop() % right);
                break;
            case OR:
                right = this.stack.pop();
                this.stack.push(toDouble(toBool(this.stack.pop()) || toBool(right)));
                break;
            case AND:
                right = this.stack.pop();
                this.stack.push(toDouble(toBool(this.stack.pop()) && toBool(right)));
                break;
            case EQ:
                this.stack.push(toDouble(this.stack.pop() == this.stack.pop()));
                break;
            case NEQ:
                this.stack.push(toDouble(this.stack.pop() != this.stack.pop()));
                break;
            case GT:
                right = this.stack.pop();
                this.stack.push(toDouble(this.stack.pop() > right));
                break;
            case GE:
                right = this.stack.pop();
                this.stack.push(toDouble(this.stack.pop() >= right));
                break;
            case LT:
                right = this.stack.pop();
                this.stack.push(toDouble(this.stack.pop() < right));
                break;
            case LE:
                right = this.stack.pop();
                this.stack.push(toDouble(this.stack.pop() <= right));
                break;
            case LABEL:
                return -2;
            case BR:
                return instr.intArg();
            case BR_ZERO:
                if (toBool(this.stack.pop()) == false) {
                    return instr.intArg();
                }
                break;
            case BR_NONZERO:
                if (toBool(this.stack.pop())) {
                    return instr.intArg();
                }
                break;
        }

        return -1;
    }

    private double loadRegister(String strArg) {
        switch (strArg) {
            case "AIM":     return this.runtime.readAim();
            case "RADAR":   return this.runtime.readRadar();
            case "SPEEDX":  return this.runtime.readSpeedX();
            case "SPEEDY":  return this.runtime.readSpeedY();
            case "X":       return this.runtime.readX();
            case "Y":       return this.runtime.readY();
            case "DAMAGE":  return this.runtime.readDamage();
            case "SHOT":    return this.runtime.readShot();
            case "RANDOM":  return this.runtime.getRandom();
            default:
                throw new IllegalArgumentException("Unknown register: '" + strArg + "'");
        }
    }

    private void storeRegister(String strArg, double d) {
        switch (strArg) {
            case "AIM":     this.runtime.writeAim(d); break;
            case "RADAR":   this.runtime.writeRadar(d); break;
            case "SPEEDX":  this.runtime.writeSpeedX(d); break;
            case "SPEEDY":  this.runtime.writeSpeedY(d); break;
            case "SHOT":    this.runtime.writeShot(d); break;
            default:
                throw new IllegalArgumentException("Unknown register: '" + strArg + "'");
        }
    }

    private static boolean toBool(double d) {
        return d != 0.0;
    }

    private static double toDouble(boolean b) {
        return b ? 1.0 : 0.0;
    }

    private static class Stack {
        final double[] array = new double[64];
        int tail;

        void push(double d) throws StackException {
            if (this.tail >= array.length) {
                throw new StackException("Stack overflow");
            }
            this.array[this.tail] = d;
            this.tail++;
        }

        double pop() throws StackException {
            if (this.tail <= 0) {
                throw new StackException("Stack underflow");
            }
            this.tail--;
            return this.array[this.tail];
        }

        double get(int i) {
            return this.array[i];
        }

        void set(int i, double d) {
            this.array[i] = d;
        }
    }

    public static class StackException extends Exception {
        private StackException(String message) {
            super(message);
        }
    }
}