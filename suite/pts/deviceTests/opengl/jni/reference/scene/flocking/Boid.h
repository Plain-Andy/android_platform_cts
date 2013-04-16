/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
// An implementation of Craig Reynold's Boid Simulation.
#ifndef BOID_H
#define BOID_H

#include <graphics/Vector2D.h>

class Boid {
public:
    Boid(float x, float y);
    void resetAcceleration();
    void flock(const Boid* boids[], int numBoids, int index, float limitX, float limitY);
    static const float MAX_SPEED = 2.0f;
    static const float MAX_FORCE = 0.05f;
    static const float NEIGHBOUR_RADIUS = 70.0f;//50
    static const float DESIRED_BOID_DIST = 30.0f;//25
    static const float SEPARATION_WEIGHT = 2.0f;
    static const float ALIGNMENT_WEIGHT = 1.0f;
    static const float COHESION_WEIGHT = 1.0f;
    Vector2D mPosition;
    Vector2D mVelocity;
    Vector2D mAcceleration;
};
#endif