package org.czbiohub.royerlab;

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
