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

package de.static_interface.reallifeplugin.module.level;

import static de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.DateUtil;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.module.level.condition.LevelConditions;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.exception.NotEnoughArgumentsException;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.MathUtil;
import eu.adventuria.adventuriaplugin.api.ResponseReceivedListener;
import eu.adventuria.adventuriaplugin.api.forum.ForumAPI;
import eu.adventuria.adventuriaplugin.api.forum.ForumRequestCode;
import eu.adventuria.adventuriaplugin.api.forum.model.response.UserStatsResponse;
import eu.adventuria.adventuriaplugin.api.model.ApiRequest;
import org.apache.commons.cli.ParseException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LevelCommand extends ModuleCommand<LevelModule> {

    public LevelCommand(LevelModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        final IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);
        final Level userLevel = LevelUtil.getLevel(user);
        final Level nextLevel = Level.Cache.getLevel(userLevel.getLevelId() + 1);

        switch (args[0]) {
            case "info":
                if (Module.isPluginAvailable("AdventuriaPlugin")) {
                    if (!ForumAPI.isLoggedIn(user)) {
                        user.sendMessage(m("Level.NotLoggedIn"));
                        return true;
                    }
                    sendCheckList(user);
                }

                break;


            case "next":
                if (nextLevel != null) {
                    user.sendMessage("&Next level: " + nextLevel.getLevelId());
                } else {
                    user.sendMessage("&4Max level reached!");
                }

                final LevelConditions conditions = nextLevel.getLevelConditions();

                ForumAPI.sendRequestAsync(new ApiRequest(ForumRequestCode.REQUEST_USER_STATS, ForumAPI.getApiKey(user)), UserStatsResponse.class,
                                          user,
                                          new ResponseReceivedListener<UserStatsResponse>() {
                                              @Override
                                              public void onReceiveResponse(UserStatsResponse response) {
                                                  boolean canlevelup = LevelUtil.canLevelUp(user, conditions, response);

                                                  if (!user.addBalance(-conditions.getCost())) {
                                                      canlevelup = false;
                                                  }

                                                  if (!canlevelup) {
                                                      user.sendMessage("&4 Can't level up! You have to met these conditions:");
                                                      sendCheckList(user);
                                                  } else {
                                                      LevelUtil.setLevel(user, nextLevel);
                                                      user.sendMessage("&a Level up! Current level: " + nextLevel.getLevelName());
                                                  }
                                              }

                                              @Override
                                              public void onResponseFailed(Throwable throwable) {
                                                  throwable.printStackTrace();
                                                  user.sendMessage("&4An internal error occured");
                                              }
                                          });
                break;

            default:
                return false;
        }

        return true;
    }

    public void sendCheckList(final IngameUser user) {
        ForumAPI.sendRequestAsync(new ApiRequest(ForumRequestCode.REQUEST_USER_STATS, ForumAPI.getApiKey(user)), UserStatsResponse.class,
                                  user, new ResponseReceivedListener<UserStatsResponse>() {
                    @Override
                    public void onReceiveResponse(UserStatsResponse response) {
                        final Level userLevel = LevelUtil.getLevel(user);
                        final Level nextLevel = Level.Cache.getLevel(userLevel.getLevelId() + 1);

                        user.sendMessage("a4Your current level: &c" + userLevel.getLevelName());
                        boolean noneAvailable = true;
                        if (nextLevel != null) {
                            user.sendMessage("&4Next level (" + nextLevel.getLevelName() + ") checklist:");
                            LevelConditions conditions = nextLevel.getLevelConditions();
                            if (conditions.getRequiredActivityPoints() > 0) {
                                user.sendMessage(toCheckList("ActivityPoints", conditions.getRequiredActivityPoints(), response.activityPoints));
                                noneAvailable = false;
                            }

                            if (conditions.getRequiredLikes() > 0) {
                                user.sendMessage(toCheckList("Likes", conditions.getRequiredLikes(), response.likesReceived));
                                noneAvailable = false;
                            }

                            if (conditions.getCost() > 0) {
                                user.sendMessage(toCheckList("Cost", conditions.getCost(), user.getBalance()));
                                noneAvailable = false;
                            }

                            if (conditions.getRequiredPosts() > 0) {
                                user.sendMessage(toCheckList("Posts", conditions.getRequiredPosts(), response.posts));
                                noneAvailable = false;
                            }

                            if (conditions.getRequiredTime() > 0) {
                                user.sendMessage(toCheckList("Time", conditions.getRequiredTime(), LevelUtil.getPlayTime(user), true));
                                noneAvailable = false;
                            }

                            if (conditions.getRequiredPermission() != null) {
                                user.sendMessage(toCheckList("Permission", conditions.getRequiredPermission(),
                                                             user.hasPermission(conditions.getRequiredPermission())));
                                noneAvailable = false;
                            }
                        } else {
                            user.sendMessage("&4Max level reached!");
                            return;
                        }
                        if (noneAvailable) {
                            user.sendMessage("-");
                        }
                    }

                    @Override
                    public void onResponseFailed(Throwable throwable) {
                        throwable.printStackTrace();
                        user.sendMessage("&4An internal error occured");
                    }
                });
    }

    private String toCheckList(String name, Number requiredValue, Number value) {
        return toCheckList(name, requiredValue, value, false);
    }

    private String toCheckList(String name, Number requiredValue, Number value, boolean isDate) {
        String s = "" + (isDate ? requiredValue : DateUtil.formatDateDiff(requiredValue.longValue()));
        boolean done = value.longValue() >= requiredValue.longValue();
        String left;
        if (isDate) {
            left = DateUtil.formatDateDiff(requiredValue.longValue() - value.longValue());
        } else {
            left = "" + MathUtil.round(requiredValue.longValue() - value.longValue());
        }
        if (!done) {
            s += " (" + left + " left)";
        }
        return toCheckList(name, s, done);
    }

    private String toCheckList(String name, String value, boolean done) {
        String s = "•" + name + ": " + value;
        s = appendCheck(s, done);
        return s;
    }

    private String appendCheck(String s, boolean value) {
        s += " " + (value ? "☑" : "☐");
        return s;
    }
}
