package org.usfirst.frc.team5987.robot.commands;

import org.usfirst.frc.team5987.robot.Robot;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.command.CommandGroup;

/**
 *
 */
public class EatCubeGroupCommand extends CommandGroup {
	
    public EatCubeGroupCommand() {
        // Add Commands here:
        // e.g. addSequential(new Command1());
        //      addSequential(new Command2());
        // these will run in order.

        // To run multiple commands at the same time,
        // use addParallel()
        // e.g. addParallel(new Command1());
        //      addSequential(new Command2());
        // Command1 and Command2 will run in parallel.

        // A command group will require all of the subsystems that each member
        // would require.
        // e.g. if Command1 requires chassis, and Command2 requires arm,
        // a CommandGroup containing them would require both the chassis and the
        // arm.
    	char scalePosition = DriverStation.getInstance().getGameSpecificMessage().charAt(0);    	
    	addSequential(new ChangeFilterModeCommand(ChangeFilterModeCommand.Modes.CUBE));
    	addSequential(new WaitToTargetCommand(Robot.ntVisionTarget, 2));
    	addParallel(new TakeCommand());
    	addSequential(new EatCubeCommand());
    	addSequential(new TurnCommand(scalePosition == 'L' ? 15 : -15, true));
    	addSequential(new ChangeFilterModeCommand(ChangeFilterModeCommand.Modes.STREAM));
    }
}