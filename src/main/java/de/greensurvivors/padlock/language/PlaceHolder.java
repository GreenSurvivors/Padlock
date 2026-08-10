package de.greensurvivors.padlock.language;

import org.intellij.lang.annotations.Subst;

/**
 * placeholder strings used. will be surrounded in Minimassage typical format of <>
 */
public enum PlaceHolder {
    ARGUMENT("argument"),
    PLAYER("player"),
    TIME("time");

    private final String placeholder;

    PlaceHolder(String placeholder) {
        this.placeholder = placeholder;
    }

    /**
     * Since this will be used in Mini-messages placeholder only the pattern "[!?#]?[a-z0-9_-]*" is valid.
     * if used inside an unparsed text you have to add surrounding <> yourself.
     */
    @Subst("name") // substitution; will be inserted if the IDE/compiler tests if input is valid.
    public String getPlaceholder() {
        return placeholder;
    }
}
