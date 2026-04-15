package ru.nsu.ccfit.factory;

public class Auto extends  Component{
    private final Body      body;
    private final Motor     motor;
    private final Accessory accessory;

    public Auto(long id, Body body, Motor motor, Accessory accessory) {
        super(id);
        this.body = body;
        this.motor = motor;
        this.accessory = accessory;
    }

    public Body getBody() { return body; }
    public Motor getMotor() { return motor; }
    public Accessory getAccessory() { return accessory; }
}
