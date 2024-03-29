/*-
 * #%L
 * Ultrack: Large-Scale Multi-Hypotheses Cell Tracking Using Ultrametric Contours Maps.
 * %%
 * Copyright (C) 2010 - 2024 RoyerLab.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.io.File;

public class CondaEnvironment {
    private final String path;
    private final boolean foundUltrack;

    public CondaEnvironment(String path) {
        this.path = path;
        this.foundUltrack = new File(path + "/bin/ultrack").exists();
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        if (foundUltrack) {
            return path + " [Ultrack found]";
        } else {
            return path + " [Ultrack not found]";
        }
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CondaEnvironment)) {
            return false;
        }
        CondaEnvironment c = (CondaEnvironment) o;
        return c.getPath().equals(this.getPath());
    }
}
