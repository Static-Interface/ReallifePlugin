package de.static_interface.reallifeplugin.entries;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.model.Entry;
import de.static_interface.reallifeplugin.model.Group;
import de.static_interface.sinklibrary.User;

public class InsuranceEntry extends Entry
{
    User user;
    Group group;

    public InsuranceEntry(User user, Group group)
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
        return "Versicherung";
    }

    @Override
    public double getAmount()
    {
        return -750;
    }

    @Override
    public boolean sendToTarget()
    {
        return true;
    }

    @Override
    public String target()
    {
        return ReallifeMain.getSettings().getInsuranceAccount();
    }
}
