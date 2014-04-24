package de.static_interface.reallifeplugin.model;

public abstract class Entry
{
    public abstract String from();

    public abstract String getReason();

    public abstract double getAmount();

    public abstract boolean sendToTarget();

    public abstract String target();
}
