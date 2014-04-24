package de.static_interface.reallifeplugin.entries;

import de.static_interface.reallifeplugin.MathHelper;
import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.model.Entry;
import de.static_interface.reallifeplugin.model.Group;
import de.static_interface.sinklibrary.User;

public class PayDayEntry extends Entry
{
    User user;
    Group group;

    public PayDayEntry(User user, Group group)
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
        return String.format("Lohn [%s]", group.name);
    }

    @Override
    public double getAmount()
    {
        return MathHelper.round(group.payday);
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
