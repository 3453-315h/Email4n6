/*
 * This file is part of Email4n6.
 * Copyright (C) 2018  Marten4n6
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.email4n6.view.tabs.tree;

import com.github.email4n6.message.factory.MessageFactory;
import com.github.email4n6.model.casedao.Case;
import com.github.email4n6.view.messagepane.MessagePane;
import com.github.email4n6.view.messagepane.MessagePaneController;
import com.github.email4n6.view.tabs.spi.GlobalTab;
import com.github.email4n6.view.tabs.tree.checktreeview.CheckTreeView;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab implementation which shows a file tree.
 *
 * @author Marten4n6
 */
@Slf4j
public class TreeTab implements GlobalTab {

    private @Getter MessagePane messagePane;
    private @Getter MessageFactory messageFactory;
    private @Getter CheckTreeView<TreeObject> tree;
    private @Getter CheckBoxTreeItem<TreeObject> rootTreeItem;

    private @Setter EventHandler<ActionEvent> onInitialize;
    private @Setter ChangeListener<TreeItem<TreeObject>> onSelectionChange;
    private @Setter ListChangeListener<TreeItem<TreeObject>> onCheckedChange;

    @Override
    public Tab getTab(Case currentCase, MessageFactory messageFactory) {
        this.messageFactory = messageFactory;

        Tab tab = new Tab();
        BorderPane tabLayout = new BorderPane();
        messagePane = new MessagePane(currentCase);

        new MessagePaneController(messagePane, messageFactory);

        // Tab
        tab.setText("Tree");
        tab.setClosable(false);
        tab.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/tree.png"))));
        tab.setContent(tabLayout);

        // Tree
        rootTreeItem = new CheckBoxTreeItem<>(new TreeObject("File(s)", null));
        tree = new CheckTreeView<>(rootTreeItem);

        rootTreeItem.setExpanded(true);

        // Split Pane
        SplitPane splitPane = new SplitPane();

        splitPane.setDividerPositions(0.18); // Between 0.00 and 1.00
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().addAll(tree, messagePane.getPane());

        // Listeners
        tree.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends TreeItem<TreeObject>> observable, TreeItem<TreeObject> oldValue, TreeItem<TreeObject> newValue) -> {
            onSelectionChange.changed(observable, oldValue, newValue);
        });
        tree.getCheckModel().getCheckedItems().addListener((ListChangeListener.Change<? extends TreeItem<TreeObject>> change) -> {
            onCheckedChange.onChanged(change);
        });

        // Add
        tabLayout.setCenter(splitPane);

        onInitialize.handle(new ActionEvent());
        return tab;
    }
}
