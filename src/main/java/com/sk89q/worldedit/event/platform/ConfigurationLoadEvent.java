/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.event.platform;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.event.Event;

/**
 * Raised when the configuration has been loaded or re-loaded.
 */
public class ConfigurationLoadEvent extends Event {

    private final LocalConfiguration configuration;

    /**
     * Create a new instance.
     *
     * @param configuration the new configuration
     */
    public ConfigurationLoadEvent(LocalConfiguration configuration) {
        checkNotNull(configuration);
        this.configuration = configuration;
    }

    /**
     * Get the configuration.
     *
     * @return the configuration
     */
    public LocalConfiguration getConfiguration() {
        return configuration;
    }

}
