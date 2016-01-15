/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.linkbubble.physics;

public interface Draggable {
    public DraggableHelper getDraggableHelper();
    public void update(float dt);
    public void onOrientationChanged();
    public boolean isDragging();

    public interface OnUpdateListener {
        public void onUpdate(Draggable draggable, float dt);
    }
}
