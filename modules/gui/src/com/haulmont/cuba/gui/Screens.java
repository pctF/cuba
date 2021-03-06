/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.haulmont.cuba.gui;

import com.haulmont.cuba.gui.components.mainwindow.AppWorkArea;
import com.haulmont.cuba.gui.screen.FrameOwner;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.ScreenOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Interface defining methods for creation and displaying of UI screens.
 *
 * @see EditorScreens
 * @see LookupScreens
 */
public interface Screens {

    /**
     * Creates a screen by its controller class.
     * <p>
     * By default, the screen will be opened in the current tab of the main window ({@link OpenMode#THIS_TAB}).
     *
     * @param screenClass screen controller class
     */
    default <T extends Screen> T create(Class<T> screenClass) {
        return create(screenClass, OpenMode.THIS_TAB, FrameOwner.NO_OPTIONS);
    }

    /**
     * Creates a screen by its controller class.
     *
     * @param screenClass screen controller class
     * @param launchMode  how the screen should be opened
     */
    default <T extends Screen> T create(Class<T> screenClass, LaunchMode launchMode) {
        return create(screenClass, launchMode, FrameOwner.NO_OPTIONS);
    }

    /**
     * Creates a screen by its screen id.
     *
     * @param screenId   screen id
     * @param launchMode how the screen should be opened
     */
    default Screen create(String screenId, LaunchMode launchMode) {
        return create(screenId, launchMode, FrameOwner.NO_OPTIONS);
    }

    /**
     * Creates a screen by its controller class.
     *
     * @param screenClass screen controller class
     * @param launchMode  how the screen should be opened
     * @param options     screen parameters
     */
    <T extends Screen> T create(Class<T> screenClass, LaunchMode launchMode, ScreenOptions options);

    /**
     * Creates a screen by its screen id.
     *
     * @param screenId   screen id
     * @param launchMode how the screen should be opened
     * @param options    screen parameters
     */
    Screen create(String screenId, LaunchMode launchMode, ScreenOptions options);

    /**
     * Displays the given screen according to its {@link LaunchMode}.
     */
    void show(Screen screen);

    // todo implement max tab count in menu components
    // todo check if mode SINGLE and close previous screens first
    // todo check multi open
//    boolean show(Screen screen);

    /**
     * Removes screen from UI and releases all the resources of screen.
     *
     * @param screen screen
     */
    void remove(Screen screen);

    /**
     * Removes all child screens (screens of work area and dialog screens) from the root screen and releases their resources.
     */
    void removeAll();

    /**
     * Check if there are screens that have unsaved changes.
     *
     * @return true if there are screens with unsaved changes
     */
    boolean hasUnsavedChanges();

    /**
     * @return object that provides information about opened screens
     */
    OpenedScreens getOpenedScreens();

    /**
     * Marker interface for screen launch modes.
     *
     * @see com.haulmont.cuba.gui.screen.OpenMode
     */
    interface LaunchMode {
    }

    /**
     * Represents single tab / window stack in {@link AppWorkArea}.
     */
    interface WindowStack {
        /**
         * @return screens of the container in descending order, first element is active screen
         * @throws IllegalStateException in case window stack has been closed
         */
        Collection<Screen> getBreadcrumbs();

        /**
         * @return true if either window stack tab is selected or if {@link AppWorkArea.Mode#SINGLE} mode is enabled.
         */
        boolean isSelected();

        /**
         * Select tab in tabbed UI.
         */
        void select();
    }

    /**
     * Provides information about opened screens, does not store state. <br>
     * Each method obtains current info from UI components tree.
     */
    interface OpenedScreens {
        /**
         * @return the root screen of UI
         * @throws IllegalStateException in case there is no root screen in UI
         */
        @Nonnull
        Screen getRootScreen();

        /**
         * @return the root screen or null
         */
        @Nullable
        Screen getRootScreenOrNull();

        /**
         * @return all opened screens excluding the root screen
         * @throws IllegalStateException if there is no root screen or root screen does not have {@link AppWorkArea}
         */
        Collection<Screen> getAll();

        /**
         * @return all opened screens excluding the root screen
         * @throws IllegalStateException if there is no root screen or root screen does not have {@link AppWorkArea}
         */
        Collection<Screen> getWorkAreaScreens();

        /**
         * @return top screens from work area tabs and all dialog windows
         * @throws IllegalStateException if there is no root screen or root screen does not have {@link AppWorkArea}
         */
        Collection<Screen> getActiveScreens();

        /**
         * @return top screens from work area tabs
         * @throws IllegalStateException if there is no root screen or root screen does not have {@link AppWorkArea}
         */
        Collection<Screen> getActiveWorkAreaScreens();

        /**
         * @return all dialog screens
         */
        Collection<Screen> getDialogScreens();

        /**
         * @return screens of the currently opened tab of work area in descending order, first element is active screen
         * @throws IllegalStateException if there is no root screen or root screen does not have {@link AppWorkArea}
         */
        Collection<Screen> getCurrentBreadcrumbs();

        /**
         * @return tab containers or single window container with access to breadcrumbs
         */
        Collection<WindowStack> getWorkAreaStacks();
    }
}