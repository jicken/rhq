/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceGroupTitleBar extends LocatableVLayout {
    private static final String FAV_ICON = "Favorite_24_Selected.png";
    private static final String NOT_FAV_ICON = "Favorite_24.png";

    private static final String COLLAPSED_TOOLTIP = MSG.view_titleBar_group_summary_collapsedTooltip();
    private static final String EXPANDED_TOOLTIP = MSG.view_titleBar_group_summary_expandedTooltip();

    private ResourceGroup group;
    private ResourceGroupComposite groupComposite;
    boolean isAutoGroup;

    private Img expandCollapseArrow;
    private Img badge;
    private Img favoriteButton;
    private HTMLFlow title;
    private Img availabilityImage;
    private boolean favorite;
    private GeneralProperties generalProperties;

    public ResourceGroupTitleBar(String locatorId, boolean isAutoGroup) {
        super(locatorId);

        this.isAutoGroup = isAutoGroup;

        setWidth100();
        setHeight(30);
        setPadding(5);
        setMembersMargin(5);
    }

    public void update() {
        for (Canvas child : getChildren()) {
            child.destroy();
        }

        final LocatableHLayout hlayout = new LocatableHLayout(extendLocatorId("hlayout"));
        addMember(hlayout);

        this.title = new HTMLFlow();
        this.title.setWidth("*");

        this.availabilityImage = new Img(ImageManager.getAvailabilityLargeIcon(null), 24, 24);

        this.favoriteButton = new LocatableImg(this.extendLocatorId("Favorite"), NOT_FAV_ICON, 24, 24);

        this.favoriteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                Set<Integer> favorites = toggleFavoriteLocally();
                UserSessionManager.getUserPreferences().setFavoriteResources(favorites, new UpdateFavoritesCallback());
            }
        });

        expandCollapseArrow = new Img("[SKIN]/ListGrid/row_collapsed.png", 16, 16);
        expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
        expandCollapseArrow.setLayoutAlign(VerticalAlignment.BOTTOM);
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(this.group.getId());
        // for autogroups we need to add more criteria
        if (isAutoGroup) {
            criteria.addFilterVisible(null);
            criteria.addFilterPrivate(true);
        }
        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                @Override
                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    if (result == null || result.size() != 1) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_titleBar_group_failInfo(group.getName(), String
                                .valueOf(ResourceGroupTitleBar.this.group.getId())));
                        return;
                    }

                    ResourceGroupComposite resultComposite = result.get(0);
                    setGroupIcons(resultComposite);

                    generalProperties = new GeneralProperties(extendLocatorId("genProps"), resultComposite);
                    generalProperties.setVisible(false);
                    ResourceGroupTitleBar.this.addMember(generalProperties);
                    expandCollapseArrow.addClickHandler(new ClickHandler() {
                        private boolean collapsed = true;

                        @Override
                        public void onClick(ClickEvent event) {
                            collapsed = !collapsed;
                            if (collapsed) {
                                expandCollapseArrow.setSrc("[SKIN]/ListGrid/row_collapsed.png");
                                expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
                                generalProperties.hide();
                            } else {
                                expandCollapseArrow.setSrc("[SKIN]/ListGrid/row_expanded.png");
                                expandCollapseArrow.setTooltip(EXPANDED_TOOLTIP);
                                generalProperties.show();
                            }
                            ResourceGroupTitleBar.this.markForRedraw();
                        }
                    });
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_titleBar_group_failInfo(group.getName(), String
                            .valueOf(ResourceGroupTitleBar.this.group.getId())), caught);
                }
            });

        badge = new Img(ImageManager.getGroupLargeIcon(GroupCategory.MIXED), 24, 24);

        TagEditorView tagEditorView = new TagEditorView(extendLocatorId("Editor"), group.getTags(), false,
            new TagsChangedCallback() {
                public void tagsChanged(final HashSet<Tag> tags) {
                    GWTServiceLookup.getTagService().updateResourceGroupTags(group.getId(), tags,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(
                                    MSG.view_titleBar_common_updateTagsFailure(group.getName()), caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_titleBar_common_updateTagsSuccessful(group.getName()),
                                        Message.Severity.Info));
                                // update what is essentially our local cache
                                group.setTags(tags);
                            }
                        });
                }
            });

        loadTags(tagEditorView);

        hlayout.addMember(expandCollapseArrow);
        hlayout.addMember(badge);
        hlayout.addMember(title);
        hlayout.addMember(availabilityImage);
        hlayout.addMember(favoriteButton);
        addMember(tagEditorView);
    }

    private void loadTags(final TagEditorView tagEditorView) {
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(group.getId());
        criteria.addFilterVisible(null); // default is only visible groups, null to support auto-cluster-groups
        criteria.fetchTags(true);

        GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroup>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_titleBar_common_loadTagsFailure(group.getName()),
                        caught);
                }

                public void onSuccess(PageList<ResourceGroup> result) {
                    LinkedHashSet<Tag> tags = new LinkedHashSet<Tag>();
                    tags.addAll(result.get(0).getTags());
                    tagEditorView.setTags(tags);
                }
            });
    }

    public void setGroup(ResourceGroupComposite groupComposite) {
        this.group = groupComposite.getResourceGroup();
        update();

        this.title.setContents("<span class=\"SectionHeader\">" + group.getName()
            + "</span>&nbsp;<span class=\"subtitle\">" + group.getGroupCategory().name() + "</span>");

        Set<Integer> favorites = UserSessionManager.getUserPreferences().getFavoriteResourceGroups();
        this.favorite = favorites.contains(group.getId());
        updateFavoriteButton();

        setGroupIcons(groupComposite);
        markForRedraw();
    }

    private void setGroupIcons(ResourceGroupComposite groupComposite) {
        Double avails = groupComposite.getExplicitAvail();
        this.badge.setSrc(ImageManager.getGroupLargeIcon(this.group.getGroupCategory(), avails));
        this.availabilityImage.setSrc(ImageManager.getAvailabilityGroupLargeIcon(avails));
    }

    private void updateFavoriteButton() {
        this.favoriteButton.setSrc(favorite ? FAV_ICON : NOT_FAV_ICON);
        if (favorite) {
            this.favoriteButton.setTooltip(MSG.view_titleBar_common_clickToRemoveFav());
        } else {
            this.favoriteButton.setTooltip(MSG.view_titleBar_common_clickToAddFav());
        }
    }

    private Set<Integer> toggleFavoriteLocally() {
        this.favorite = !this.favorite;
        Set<Integer> favorites = UserSessionManager.getUserPreferences().getFavoriteResourceGroups();
        if (this.favorite) {
            favorites.add(group.getId());
        } else {
            favorites.remove(group.getId());
        }
        return favorites;
    }

    public class UpdateFavoritesCallback implements AsyncCallback<Subject> {
        public void onSuccess(Subject subject) {
            String m;
            if (favorite) {
                m = MSG.view_titleBar_common_addedFav(ResourceGroupTitleBar.this.group.getName());
            } else {
                m = MSG.view_titleBar_common_removedFav(ResourceGroupTitleBar.this.group.getName());
            }
            CoreGUI.getMessageCenter().notify(new Message(m, Message.Severity.Info));
            updateFavoriteButton();
        }

        public void onFailure(Throwable throwable) {
            String m;
            if (favorite) {
                m = MSG.view_titleBar_common_addedFavFailure(ResourceGroupTitleBar.this.group.getName());
            } else {
                m = MSG.view_titleBar_common_removedFavFailure(ResourceGroupTitleBar.this.group.getName());
            }
            CoreGUI.getMessageCenter().notify(new Message(m, Message.Severity.Error));
            // Revert back to our original favorite status, since the server update failed.
            toggleFavoriteLocally();
        }
    }
}