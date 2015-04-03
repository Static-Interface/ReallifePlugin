/*
 * Copyright (c) 2013 - 2015 <http://static-interface.de> and contributors
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.reallifeplugin.module.contract;

import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

//todo: i18n

public class ContractConversation {

    public final static String DATE_PATTERN = "d.M.yyyy HH:MM";

    public static void createNewView(Player player, Plugin plugin) {
        ConversationFactory factory = new ConversationFactory(plugin).withModality(true)
                // .withPrefix(new Prefix())
                .withFirstPrompt(new Welcome())
                .withEscapeSequence("quit");
        Conversation conv = factory.buildConversation(player);
        conv.getContext().setSessionData(ContractOption.PLAYER, player.getUniqueId().toString());
        Map<String, Integer> moneyAmount = new HashMap<>();
        conv.getContext().setSessionData(ContractOption.TARGETS, new ArrayList<>());
        conv.getContext().setSessionData(ContractOption.MONEY_AMOUNTS, moneyAmount);
        conv.begin();
    }

    public static Player getPlayer(ConversationContext context) {
        return (Player) context.getForWhom();
    }

    private static class Welcome extends MessagePrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&6-------------------------------\n" +
                                                          "&8>&fWillkommen beim &6Vertragsetup&f!\n" +
                                                          "&8>&7Dieses Setup wird dich beim erstellen eines Vertrages begleiten.\n" +
                                                          "&8>&7Mit &6&lquit&r&7 im Chat kannst du abbrechen.");
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new NamePrompt();
        }
    }

    private static class ErrorPrompt extends MessagePrompt {

        private String message;
        private Prompt nextPrompt;

        public ErrorPrompt(Prompt nextPrompt, String message) {
            this.message = message;
            this.nextPrompt = nextPrompt;
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return nextPrompt;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&', "&c>&4" + message);
        }
    }

    private static class NamePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7>&6>&7Wie soll der Vertrag heißen? (Min. 5 Zeichen)!");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();
            if (input.equals("") || input.length() < 5) {
                return this;
            }

            input = input.replace(" ", "_");
            //Todo: check if name already exists

            context.setSessionData(ContractOption.NAME, input);
            return new AddUserPrompt();
        }
    }

    private static class AddUserPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7>&6>&7Welche Personen willst du hinzufügen ? (Getrennt mit einem Komma) \n" +
                                                          "&8>&7Beispiel: SpielerA, SpielerB, SpielerC");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.trim().equals("")) {
                return this;
            }

            String[] rawPlayers = input.split(",");

            List<String> players = (List<String>) context.getSessionData(ContractOption.TARGETS);
            for (String s : rawPlayers) {
                s = s.trim();
                IngameUser user = SinkLibrary.getInstance().getIngameUser(s);
                if (user.getName().equalsIgnoreCase(getPlayer(context).getName())) {
                    return new ErrorPrompt(this, "Du kannst dich nicht selbst hinzufuegen");
                }
                if (!user.hasPlayedBefore()) {
                    return new ErrorPrompt(this, "Unbekannter Spieler: &c" + s);
                }
                players.add(user.getUniqueId().toString());
            }

            context.setSessionData(ContractOption.TARGETS, players);
            return new AddMoreUsersPrompt();
        }
    }

    private static class AddMoreUsersPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7>&6>&7Moechtest du weitere Spieler hinzufuegen? &6[J]a&7/&6[N]ein");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();

            if (input.equalsIgnoreCase("ja") || input.equalsIgnoreCase("j")) {
                return new AddUserPrompt();
            } else if (input.equalsIgnoreCase("nein") || input.equalsIgnoreCase("n")) {
                return new TypePrompt();
            }

            return new ErrorPrompt(this, "Ungueltige Option: &c" + input);
        }
    }

    private static class TypePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Bitte wählen einen Vertragstyp:\n" +
                                                          "&8>&7>&6> &6PERIODIC &7- Periodischer Vetrag, mit dem zB wöchentlich Geld abheben kann (zB fuer GS Vermietungen)\n"
                                                          +
                                                          "&8>&7>&6> &6NORMAL &7- Normaler Vertrag, mit dem man zB ein Grundstück verkaufen kann");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();

            ContractType type = ContractType.valueOf(input.toUpperCase());
            if (type == null) {
                return new ErrorPrompt(this, "Ungueltiger Typ: &c" + input);
            }

            context.setSessionData(ContractOption.TYPE, input.toUpperCase());
            return new ExpirePrompt();
        }
    }

    private static class ExpirePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7>&6>&7Wann soll der Vertrag ablaufen?:\n" +
                                                          "&8>&7Beispiel: &625.08.2015 20:00 &7(<-- dieses Format nutzen)");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();

            if (input.equals("")) {
                return this;
            }

            try {
                new SimpleDateFormat(DATE_PATTERN, Locale.GERMAN).parse(input);
            } catch (Exception e) {
                return new ErrorPrompt(this, "Ungueltiges Datum: &c" + input);
            }

            context.setSessionData(ContractOption.EXPIRE, input);
            return new EventPrompt();
        }
    }

    private static class EventPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Wähle einen Eventtyp (tritt nach Ablauf des Vertrages oder bei peridoschen Vertraegen nach Ende jeder der Periode ein):\n"
                                                          +
                                                          "&8>&7>&6> &6MONEY &7- Geld abheben\n" +
                                                          "&8>&7>&6> &6DEFAULT &7- Nichts tun");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            ContractEvent eventType = ContractEvent.valueOf(input.toUpperCase());
            ContractType contractType = ContractType.valueOf((String) context.getSessionData(ContractOption.TYPE));

            if (contractType == ContractType.PERIODIC && eventType == ContractEvent.DEFAULT) {
                return new ErrorPrompt(this, "Periodische Verträge können nicht DEFAULT-Events nutzen");
            }

            if (eventType == null) {
                return new ErrorPrompt(this, "Ungueltiger Typ: &c" + input);
            }

            context.setSessionData(ContractOption.EVENT, eventType.toString());

            switch (contractType) {
                case PERIODIC:
                    return new PeriodicPrompt();
                case NORMAL:
                default:
                    switch (eventType) {
                        case MONEY:
                            return new MoneyPrompt();
                        case DEFAULT:
                        default:
                            return new QuitPrompt();
                    }
            }
        }
    }

    private static class PeriodicPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7>&6>&7Wann soll das Event (zB Abzug des Geldes bei Mieten) stattfinden?:\n" +
                                                          "&8>&7Beispiel1: &624d 15h &7(alle 24 Tage und 15 Stunden)\n" +
                                                          "&8>&7Beispiel2: &613h 30m &7(alle 13 Stunden und 30 Minuten)");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();
            if (input.equals("")) {
                return this;
            }

            //TODO validate

            ContractEvent eventType = ContractEvent.valueOf((String) context.getSessionData(ContractOption.EVENT));
            switch (eventType) {
                case MONEY:
                    return new MoneyPrompt();
                case DEFAULT:
                default:
                    return new QuitPrompt();
            }
        }
    }

    private static class MoneyPrompt extends NumericPrompt {

        private int index;

        public MoneyPrompt() {
            this(0);
        }

        public MoneyPrompt(int i) {
            index = i;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            UUID uuid = UUID.fromString(((List<String>) context.getSessionData(ContractOption.TARGETS)).get(index));
            IngameUser user = SinkLibrary.getInstance().getIngameUser(uuid);

            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7>&6>&Wie viel Geld soll von " + user.getDisplayName()
                                                          + " &r&7abgezogen oder hinzugefügt werden?\n" +
                                                          "&8>&7&6Beispiel: &6500 &7oder auch &6-500 &7(oder &60&7 für keine Transaktionen)" +
                                                          "&8>&7Das Geld wird dem Konto des Vertragserstellers hinzugefügt oder davon entfernt.");
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
            UUID uuid = UUID.fromString(((List<String>) context.getSessionData(ContractOption.TARGETS)).get(index));
            IngameUser user = SinkLibrary.getInstance().getIngameUser(uuid);

            Map<String, Integer> money_amount = (Map<String, Integer>) context.getSessionData(ContractOption.MONEY_AMOUNTS);
            money_amount.put(user.getUniqueId().toString(), number.intValue());
            context.setSessionData(ContractOption.MONEY_AMOUNTS, money_amount);

            index++;
            if (index >= ((List<String>) context.getSessionData(ContractOption.TARGETS)).size()) {
                return new QuitPrompt();
            }
            return new MoneyPrompt(index);
        }
    }

    private static class QuitPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&', "&2>&aDer Vertrag \"&6" + ((String) context.getSessionData(ContractOption.NAME))
                    .replace("_", " ") + "&r&a\" wird erstellt, warte auf Bestaetigung der Vertragspartner (&6/caccept&7)!");
        }

        @Override
        public Prompt acceptInput(ConversationContext conversationContext, String s) {
            return END_OF_CONVERSATION;
        }
    }
}
