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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.MessagePrompt;
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
                .withEscapeSequence("/quit")
                .withTimeout(60)
                .thatExcludesNonPlayersWithMessage("You must be in game!");
        Conversation conv = factory.buildConversation(player);
        conv.getContext().setSessionData(ContextOption.PLAYER, player.getUniqueId().toString());
        Map<String, Integer> moneyAmount = new HashMap<>();
        conv.getContext().setSessionData(ContextOption.TARGETS, new ArrayList<>());
        conv.getContext().setSessionData(ContextOption.MONEY_AMOUNTS, moneyAmount);
        conv.begin();
    }

    public static Player getPlayer(ConversationContext context) {
        return Bukkit.getPlayer(UUID.fromString((String) context.getSessionData(ContextOption.PLAYER)));
    }

    private static class Welcome extends MessagePrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&6-------------------------------\n&" +
                                                          "&8>&fWillkommen beim &bVertragsetup&f!\n" +
                                                          "&8>&7Dieses Setup wird dich beim erstellen eines Vertrages begleiten\n" +
                                                          "&8>&7Mit &b/quit&7 kannst du abbrechen.");
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new NamePrompt();
        }
    }

    private static class NamePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Bitte schreibe den Namen des Vertrags (min 5 Zeichen)!");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();
            if (input.equals("") || input.length() < 5) {
                return this;
            }

            context.setSessionData(ContextOption.NAME, input.replace(" ", "_"));
            return new AddUserPrompt();
        }
    }

    private static class AddUserPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Bitte schreibe den Namen der Personen, die du hinzufügen willst" +
                                                          "&8>&7Beispiel: SpielerA, SpielerB, SpielerC");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.trim().equals("")) {
                return this;
            }

            String[] rawPlayers = input.split(",");

            List<String> players = (List<String>) context.getSessionData(ContextOption.TARGETS);
            for (String s : rawPlayers) {
                s = s.trim();
                IngameUser user = SinkLibrary.getInstance().getIngameUser(s);
                if (!user.hasPlayedBefore()) {
                    getPlayer(context).sendMessage("&c> &4Unbekannter Spieler: &c" + s);
                    return this;
                }
                players.add(user.getUniqueId().toString());
            }

            context.setSessionData(ContextOption.TARGETS, players);
            return new AddMoreUsersPrompt();
        }
    }

    private static class AddMoreUsersPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Moechtest du mehr Spieler hinzufuegen? (&bJa&7/&bNein&7)");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();

            if (input.equalsIgnoreCase("ja")) {
                return new AddUserPrompt();
            } else if (input.equalsIgnoreCase("nein")) {
                return new TypePrompt();
            }

            getPlayer(context).sendMessage("&c> &4Ungueltige Antwort: &c" + input);
            return this;
        }
    }

    private static class TypePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Bitte wählen einen Vertragstyp:&f!\n" +
                                                          "&8>&7>&6> &bPERIODIC &7- Periodischer Vetrag, mit dem zB wöchentlich Geld abhoben wird (zB fuer GS Vermietungen)\n"
                                                          +
                                                          "&8>&7>&6> &bDEFAULT &7- Normaler Vertrag, mit dem zB ein Grundstück verkauft werden kann");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();

            ContractType type = ContractType.valueOf(input.toUpperCase());
            if (type == null) {
                getPlayer(context).sendMessage("&c> &4Ungueltiger Typ: &c" + input);
                return this;
            }

            context.setSessionData(ContextOption.TYPE, input.toUpperCase());
            return new ExpirePrompt();
        }
    }

    private static class ExpirePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Bitte wähle das Datum und die Uhrzeit fuer den Ablauf des Vertrages:&f!\n" +
                                                          "&8>&7Beispiel: &b25.08.2015 20:00");
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
                getPlayer(context).sendMessage("&c> &4Ungueltiges Datum: &c" + input);
                return this;
            }

            context.setSessionData(ContextOption.EXPIRE, input);
            return new EventPrompt();
        }
    }

    private static class EventPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Bitte wähle einen Eventtyp:&f!\n" +
                                                          "&8>&7>&6> &bMONEY &7- Geld abheben\n" +
                                                          "&8>&7>&6> &bDEFAULT &7- Nichts tun");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            ContractEventType eventType = ContractEventType.valueOf(input.toUpperCase());
            ContractType contractType = ContractType.valueOf((String) context.getSessionData(ContextOption.TYPE));

            if (contractType == ContractType.PERIODIC && eventType == ContractEventType.DEFAULT) {
                getPlayer(context).sendMessage("&c> &4Periodische Verträge können nicht DEFAULT nutzen");
                return this;
            }

            if (eventType == null) {
                getPlayer(context).sendMessage("&c> &4Ungueltiger Typ: &c" + input);
                return this;
            }

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
                            return new PlaceholderPrompt();
                    }
            }
        }
    }

    private static class PeriodicPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Bitte wähle die Zeitspanne, in der das Event eintreten soll:!\n" +
                                                          "&8>&7Beispiel1: &b24d 15h &7(alle 24 Tage und 15 Stunden)\n" +
                                                          "&8>&7Beispiel2: &b13h 30m &7(alle 13 Stunden und 30 Minuten)");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();
            if (input.equals("")) {
                return this;
            }

            ContractEventType eventType = ContractEventType.valueOf(input.toUpperCase());
            switch (eventType) {
                case MONEY:
                    return new MoneyPrompt();
                case DEFAULT:
                default:
                    return new PlaceholderPrompt();
            }
        }
    }

    private static class MoneyPrompt extends StringPrompt {

        private int index;

        public MoneyPrompt() {
            this(0);
        }

        public MoneyPrompt(int i) {
            index = i;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            UUID uuid = UUID.fromString(((List<String>) context.getSessionData(ContextOption.TARGETS)).get(index));
            IngameUser user = SinkLibrary.getInstance().getIngameUser(uuid);

            return ChatColor.translateAlternateColorCodes('&',
                                                          "&8>&7Bitte gib ein, wie viel Geld von " + user.getDisplayName()
                                                          + " abgezogen oder hinzugefügt werden soll:!\n" +
                                                          "&8>&7&6Beispiel: &b500 &7oder auch &b-500" +
                                                          "&8>&7&6Das Geld wird dem Konto des Vertragserstellers hinzugefügt oder davon entfernt.");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            UUID uuid = UUID.fromString(((List<String>) context.getSessionData(ContextOption.TARGETS)).get(index));
            IngameUser user = SinkLibrary.getInstance().getIngameUser(uuid);
            int amount;
            try {
                amount = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                getPlayer(context).sendMessage("&c> &4Ungueltige Zahl: &c" + input);
                return this;
            }

            Map<String, Integer> money_amount = (Map<String, Integer>) context.getSessionData(ContextOption.MONEY_AMOUNTS);
            money_amount.put(user.getUniqueId().toString(), amount);
            context.setSessionData(ContextOption.MONEY_AMOUNTS, money_amount);

            index++;
            if (index >= ((List<String>) context.getSessionData(ContextOption.TARGETS)).size()) {
                return new MoneyPrompt(index);
            }
            return new PlaceholderPrompt();
        }
    }

    private static class PlaceholderPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext conversationContext) {
            return null;
        }

        @Override
        public Prompt acceptInput(ConversationContext conversationContext, String s) {
            return null;
        }
    }
}
