package de.static_interface.reallifeplugin;

public class MathHelper
{
    public static double round(double value)
    {
        return (int) Math.round(value * 100) / (double) 100;
    }
}
