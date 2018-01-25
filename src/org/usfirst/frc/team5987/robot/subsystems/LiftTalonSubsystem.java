package org.usfirst.frc.team5987.robot.subsystems;

import org.usfirst.frc.team5987.robot.RobotMap;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.LimitSwitchNormal;
import com.ctre.phoenix.motorcontrol.LimitSwitchSource;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;


import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.command.Subsystem;



/**
 *  @author mow, paulo, Dor
 */
public class LiftTalonSubsystem extends Subsystem {
	enum States {
		/**
		 * The mechanism doesn't move
		 */
		MECHANISM_DISABLED,
		/**
		 * The mechanism goes down slowly till it meets the hall effect and zeros the encoder (define 0)
		 */
		ZEROING,
		/**
		 * Controls the position based on setSetpoint(double height)
		 */
		RUNNING
	}
	
	private static final double topPIDF[] = {
			1, // P
			0, // I
			0, // D
			0  // F
			};
	private static final double bottomPIDF[] = {
			0.5, // P
			0,   // I
			0,   // D
			0    // F
			}; 
	/**
	 * Decreasing rate for the output (substructs this from the setpoint every iteration)
	 */
	private static final double ZERO_RATE = 0.005;
	private static final double MAX_ZEROING_OUTPUT = 0.3333334;
	private static final double MAX_RUNNING_OUTPUT = 0.5;
	private static final double TICKS_PER_METER = getDistanceToTicks(0.0455, 4096, 2);
	private static final boolean TOP_HALL_REVERSED = true;
	private static final boolean BOTTOM_HALL_REVERSED = false;
	private static final int TALON_TIMEOUT_MS = 10;
	private static final int TALON_UP_PID_SLOT = 0;
	private static final int TALON_DOWN_PID_SLOT = 1;
	
	private States state = States.MECHANISM_DISABLED;
	
	public NetworkTable LiftTable = NetworkTableInstance.getDefault().getTable("liftTable");

	NetworkTableEntry ntTopHall = LiftTable.getEntry("Top Hall");
	NetworkTableEntry ntBottomHall = LiftTable.getEntry("Bottom Hall");
	NetworkTableEntry ntState = LiftTable.getEntry("State");
	NetworkTableEntry ntError = LiftTable.getEntry("Error");
	
	NetworkTableEntry ntIsEnabled = LiftTable.getEntry("IS ENABLED");
	
	NetworkTableEntry ntHeight = LiftTable.getEntry("height");

	TalonSRX liftMotor = new TalonSRX(RobotMap.liftMotorPort);
	
	double setpoint = 0;

	public LiftTalonSubsystem(){
		// The PID values are defined in the robo-rio webdash. Not NT!!!
		liftMotor.selectProfileSlot(TALON_UP_PID_SLOT, 0);
	     liftMotor.config_kP(0, topPIDF[0], TALON_TIMEOUT_MS);
		 liftMotor.config_kI(0, topPIDF[1], TALON_TIMEOUT_MS);
		 liftMotor.config_kD(0, topPIDF[2], TALON_TIMEOUT_MS);
		 liftMotor.config_kF(0, topPIDF[3], TALON_TIMEOUT_MS);
		
		liftMotor.selectProfileSlot(TALON_DOWN_PID_SLOT, 0);
	     liftMotor.config_kP(0, bottomPIDF[0], TALON_TIMEOUT_MS);
		 liftMotor.config_kI(0, bottomPIDF[1], TALON_TIMEOUT_MS);
		 liftMotor.config_kD(0, bottomPIDF[2], TALON_TIMEOUT_MS);
		 liftMotor.config_kF(0, bottomPIDF[3], TALON_TIMEOUT_MS);
		 
		
		 // Add the NetworkTable entries if they don't exist
		ntIsEnabled.setBoolean(ntIsEnabled.getBoolean(false));
		liftMotor.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, TALON_TIMEOUT_MS);
		// the limit switches are hall effects actually and are connected to the encoder
		// fwd = top
		liftMotor.configForwardLimitSwitchSource( 
				LimitSwitchSource.FeedbackConnector,
				TOP_HALL_REVERSED ? LimitSwitchNormal.NormallyClosed : LimitSwitchNormal.NormallyOpen,
				TALON_TIMEOUT_MS); 
		// reverse = bottom
		liftMotor.configReverseLimitSwitchSource(
				LimitSwitchSource.FeedbackConnector,
				BOTTOM_HALL_REVERSED ? LimitSwitchNormal.NormallyClosed : LimitSwitchNormal.NormallyOpen,
				TALON_TIMEOUT_MS);

	}
	
    public void initDefaultCommand() {
    
    }
    
    private static double getDistanceToTicks(double diameter, double ticksPerRevolution, double scale){
    	return ticksPerRevolution / (diameter * Math.PI * scale);
    }
    
    /**
     * Update the PID setpoint
     * @param height in METER
     */
    public void setSetpoint(double height) {
    	setpoint = height * TICKS_PER_METER;
    	boolean goingUp = setpoint - getHeight()  > 0;
    	if(goingUp)
    		liftMotor.selectProfileSlot(TALON_UP_PID_SLOT, 0);
    	else
    		liftMotor.selectProfileSlot(TALON_DOWN_PID_SLOT, 0);
    }
    
    private void setPosition(){
    	liftMotor.set(ControlMode.Position, setpoint);
    }
    
    /**
     * Run this periodically with setSetpoint() in order to move the motors 
     */
    public void update() {
    	switch(state){
	    	case MECHANISM_DISABLED:
	    		ntState.setString("MECHANISM_DISABLED");
	    		if(ntIsEnabled.getBoolean(false))
	    			state = States.ZEROING;
	    		liftMotor.set(ControlMode.PercentOutput, 0);
	    		break;
	    		
	    	case ZEROING:
	    		ntState.setString("ZEROING");
	    		setSetpoint(setpoint - ZERO_RATE);
	    		liftMotor.configVoltageCompSaturation(MAX_ZEROING_OUTPUT * 12, TALON_TIMEOUT_MS);
	    		setPosition();
	    		if(liftMotor.getSensorCollection().isRevLimitSwitchClosed()){
	    			state = States.RUNNING;
	    			liftMotor.setSelectedSensorPosition(0, 0, TALON_TIMEOUT_MS); // zero
	    		}
	    		break;
	    		
	    	case RUNNING:
	    		ntState.setString("RUNNING");
	    		liftMotor.configVoltageCompSaturation(MAX_RUNNING_OUTPUT * 12, TALON_TIMEOUT_MS);
	    		setPosition();
	    		if(!ntIsEnabled.getBoolean(false))
	    			state = States.MECHANISM_DISABLED;
	    		break;
	    		
	    	default:
	    		state = States.MECHANISM_DISABLED;
	    		break;
    	}
    	ntError.setDouble(getHeight());
    	ntHeight.setDouble(getSpeed());
    }
    
	public double getHeight(){
		return liftMotor.getSelectedSensorPosition(0) * TICKS_PER_METER;
	}
     
    public double getSpeed() {
    	return liftMotor.getSelectedSensorVelocity(0) * TICKS_PER_METER;
    }

   
}

