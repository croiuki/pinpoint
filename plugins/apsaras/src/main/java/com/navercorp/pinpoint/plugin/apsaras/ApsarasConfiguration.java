/*
 * Copyright 2017 DHGate Corp.
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
 */

package com.navercorp.pinpoint.plugin.apsaras;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;

import java.util.List;

/**
 * @author chenjunhua
 */
public class ApsarasConfiguration {

    private final boolean apsarasEnabled;
    private final List<String> apsarasBootstrapMains;

    public ApsarasConfiguration(ProfilerConfig config) {
        this.apsarasEnabled = config.readBoolean("profiler.apsaras.enable", true);
        this.apsarasBootstrapMains = config.readList("profiler.apsaras.bootstrap.main");
    }

    public boolean isApsarasEnabled() {
        return apsarasEnabled;
    }

    public List<String> getApsarasBootstrapMains() {
        return apsarasBootstrapMains;
    }
}
