package ru.agroexpert2007.aegis;

import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayWithIW;

import java.util.List;

public class OverlaysHandler {
    public static void removeOverlay(List<Overlay> overlayList, int overlayIndex) {
        overlayList.remove(overlayIndex);
    }
    public static void removeAllMarkers(List<OverlayWithIW> overlayList) {
        for(int i = 0; i < overlayList.size(); i++) {
            if(overlayList.get(i).getTitle().contains("Marker")) {
                overlayList.remove(i);
            }
        }
    }
}