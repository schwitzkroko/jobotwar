package net.smackem.jobotwar.runtime;

/**
 * Functional interface to log messages generated by a robot.
 */
@FunctionalInterface
public interface RobotMessageLogger {
    /**
     * Logs the message.
     * @param robot The {@link Robot} that produced the message.
     * @param category The category (e.g. register name)
     * @param value The new value of the item identified by {@code category}.
     */
    void log(Robot robot, String category, double value);
}
