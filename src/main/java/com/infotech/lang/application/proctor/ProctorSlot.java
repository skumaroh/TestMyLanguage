package com.infotech.lang.application.proctor;

import java.util.ArrayList;
import java.util.List;

/** One place a text is needed: what for, which activity, which level, which proctor language. */
public record ProctorSlot(ProctorPurpose purpose, ProctorActivity activity, int level, String proctorLanguage) {

    public static ProctorSlot greeting(int level, String proctorLanguage) {
        return new ProctorSlot(ProctorPurpose.GREETING, ProctorActivity.NONE, level, proctorLanguage);
    }

    public static ProctorSlot intro(ProctorActivity activity, int level, String proctorLanguage) {
        return new ProctorSlot(ProctorPurpose.ACTIVITY_INTRO, activity, level, proctorLanguage);
    }

    /** The greeting followed by the five activity introductions. */
    public static List<ProctorSlot> allForLevel(int level, String proctorLanguage) {
        List<ProctorSlot> slots = new ArrayList<>();
        slots.add(greeting(level, proctorLanguage));
        for (ProctorActivity activity : ProctorActivity.values()) {
            if (activity != ProctorActivity.NONE) {
                slots.add(intro(activity, level, proctorLanguage));
            }
        }
        return slots;
    }

    public String key() {
        return purpose + "|" + activity + "|" + level + "|" + proctorLanguage;
    }
}
