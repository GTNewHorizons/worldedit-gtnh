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

package com.sk89q.worldedit.function.mask;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.session.request.Request;

/**
 * Various utility functions related to {@link Mask} and {@link Mask2D}.
 */
public final class Masks {

    private static final AlwaysTrue ALWAYS_TRUE = new AlwaysTrue();
    private static final AlwaysFalse ALWAYS_FALSE = new AlwaysFalse();

    private Masks() {}

    /**
     * Return a 3D mask that always returns true;
     *
     * @return a mask
     */
    public static Mask alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     * Return a 2D mask that always returns true;
     *
     * @return a mask
     */
    public static Mask2D alwaysTrue2D() {
        return ALWAYS_TRUE;
    }

    /**
     * Negate the given mask.
     *
     * @param mask the mask
     * @return a new mask
     */
    public static Mask negate(final Mask mask) {
        if (mask instanceof AlwaysTrue) {
            return ALWAYS_FALSE;
        } else if (mask instanceof AlwaysFalse) {
            return ALWAYS_TRUE;
        }

        checkNotNull(mask);
        return new AbstractMask() {

            @Override
            public boolean test(Vector vector) {
                return !mask.test(vector);
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                Mask2D mask2d = mask.toMask2D();
                if (mask2d != null) {
                    return negate(mask2d);
                } else {
                    return null;
                }
            }
        };
    }

    /**
     * Negate the given mask.
     *
     * @param mask the mask
     * @return a new mask
     */
    public static Mask2D negate(final Mask2D mask) {
        if (mask instanceof AlwaysTrue) {
            return ALWAYS_FALSE;
        } else if (mask instanceof AlwaysFalse) {
            return ALWAYS_TRUE;
        }

        checkNotNull(mask);
        return new AbstractMask2D() {

            @Override
            public boolean test(Vector2D vector) {
                return !mask.test(vector);
            }
        };
    }

    /**
     * Return a 3-dimensional version of a 2D mask.
     *
     * @param mask the mask to make 3D
     * @return a 3D mask
     */
    public static Mask asMask(final Mask2D mask) {
        return new AbstractMask() {

            @Override
            public boolean test(Vector vector) {
                return mask.test(vector.toVector2D());
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                return mask;
            }
        };
    }

    /**
     * Wrap an old-style mask and convert it to a new mask.
     *
     * <p>
     * Note, however, that this is strongly not recommended because
     * {@link com.sk89q.worldedit.masks.Mask#prepare(LocalSession, LocalPlayer, Vector)}
     * is not called.
     * </p>
     *
     * @param mask        the old-style mask
     * @param editSession the edit session to bind to
     * @return a new-style mask
     * @deprecated Please avoid if possible
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    public static Mask wrap(final com.sk89q.worldedit.masks.Mask mask, final EditSession editSession) {
        checkNotNull(mask);
        return new AbstractMask() {

            @Override
            public boolean test(Vector vector) {
                return mask.matches(editSession, vector);
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                return null;
            }
        };
    }

    /**
     * Wrap an old-style mask and convert it to a new mask.
     *
     * <p>
     * As an {@link EditSession} is not provided in this case, one will be
     * taken from the {@link Request}, if possible. If not possible, then the
     * mask will return false.
     * </p>
     *
     * @param mask the old-style mask
     * @return a new-style mask
     */
    @SuppressWarnings("deprecation")
    public static Mask wrap(final com.sk89q.worldedit.masks.Mask mask) {
        checkNotNull(mask);
        return new AbstractMask() {

            @Override
            public boolean test(Vector vector) {
                EditSession editSession = Request.request()
                    .getEditSession();
                return editSession != null && mask.matches(editSession, vector);
            }

            @Nullable
            @Override
            public Mask2D toMask2D() {
                return null;
            }
        };
    }

    /**
     * Convert a new-style mask to an old-style mask.
     *
     * @param mask the new-style mask
     * @return an old-style mask
     */
    @SuppressWarnings("deprecation")
    public static com.sk89q.worldedit.masks.Mask wrap(final Mask mask) {
        checkNotNull(mask);
        return new com.sk89q.worldedit.masks.AbstractMask() {

            @Override
            public boolean matches(EditSession editSession, Vector position) {
                Request.request()
                    .setEditSession(editSession);
                return mask.test(position);
            }
        };
    }

    private static class AlwaysTrue implements Mask, Mask2D {

        @Override
        public boolean test(Vector vector) {
            return true;
        }

        @Override
        public boolean test(Vector2D vector) {
            return true;
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return this;
        }
    }

    private static class AlwaysFalse implements Mask, Mask2D {

        @Override
        public boolean test(Vector vector) {
            return false;
        }

        @Override
        public boolean test(Vector2D vector) {
            return false;
        }

        @Nullable
        @Override
        public Mask2D toMask2D() {
            return this;
        }
    }

}
