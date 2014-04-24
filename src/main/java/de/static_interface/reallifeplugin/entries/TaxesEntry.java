package de.static_interface.reallifeplugin.entries;

import de.static_interface.reallifeplugin.MathHelper;
import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.model.Entry;
import de.static_interface.reallifeplugin.model.Group;
import de.static_interface.sinklibrary.User;

public class TaxesEntry extends Entry
{
    User user;
    Group group;

    public TaxesEntry(User user, Group group)
    {
        this.user = user;
        this.group = group;
    }

    @Override
    public String from()
    {
        return user.getName();
    }

    @Override
    public String getReason()
    {
        return String.format("%s%% Steuern", MathHelper.round(getTaxesModifier() * 100));
    }

    @Override
    public double getAmount()
    {
        return -MathHelper.round(getTaxes());
    }

    private double getTaxesModifier()
    {
        double money = user.getMoney() + group.payday;
        double taxesBase = ReallifeMain.getSettings().getTaxesBase();

        double taxesmodifier = group.taxesmodifier;
        if ( money <= 25000 )
        {
            taxesmodifier *= 20;
        }
        else if ( money <= 50000 )
        {
            taxesmodifier *= 16;
        }
        else if ( money <= 100000 )
        {
            taxesmodifier *= 12;
        }
        else if ( money <= 250000 )
        {
            taxesmodifier *= 8;
        }
        else if ( money <= 500000 )
        {
            taxesmodifier *= 6;
        }
        else if ( money <= 750000 )
        {
            taxesmodifier *= 5;
        }
        else
        {
            taxesmodifier *= 2.5;
        }

        return (taxesBase * taxesmodifier) / 100;
    }

    private double getTaxes()
    {
        return getTaxesModifier() * (user.getMoney() + group.payday);
    }

    @Override
    public boolean sendToTarget()
    {
        return true;
    }

    @Override
    public String target()
    {
        return ReallifeMain.getSettings().getEconomyAccount();
    }
}
