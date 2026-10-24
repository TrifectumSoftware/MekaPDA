package com.trifectumsoftware.mekapda.client.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.trifectumsoftware.mekapda.network.DeviceEntry;
import com.trifectumsoftware.mekapda.network.FrequencyEntry;
import com.trifectumsoftware.mekapda.network.NetworkPayload;
import com.trifectumsoftware.mekapda.network.NetworkSummary;
import com.trifectumsoftware.mekapda.client.MekaPdaClientState;
import com.trifectumsoftware.mekapda.client.PdaClient;
import com.wf.gemrender.ui.Canvas;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class PdaView {

    private static final float SURFACE_W = 400;
    private static final float SURFACE_H = 260;

    private static final float FRAME = 10;

    private static final float W = SURFACE_W - FRAME * 2;
    private static final float H = SURFACE_H - FRAME * 2;

    public static final float PANEL_X = FRAME;
    public static final float PANEL_Y = FRAME;
    public static final float PANEL_W = W;
    public static final float PANEL_H = H;

    private static final float MARGIN = 12;
    private static final float ROW_H = 40;
    private static final float ROW_GAP = 4;
    private static final float NET_ROW_H = 40;
    private static final float NET_ROW_GAP = 3;

    private static final float BASELINE = 3.5f;

    private final float x0 = FRAME;
    private final float y0 = FRAME;

    private Tab tab = Tab.PUBLIC;
    private Sort sort = Sort.NAME;
    private FrequencyEntry selected;
    private NetworkPayload network;
    private boolean awaitingList = true;
    private boolean awaitingNetwork = false;

    private double scroll;
    private double netScroll;
    private String tooltip;
    private boolean lastPressed;

    private float transition;
    private int transitionDir;

    public void open() {
        awaitingList = true;
        MekaPdaClientState.requestFrequencies();
        if (selected != null) {
            awaitingNetwork = true;
            MekaPdaClientState.requestNetwork(selected);
        }
    }

    private void startTransition(int direction) {
        if (direction != 0) {
            transitionDir = direction;
            transition = 1.0f;
        }
    }

    public void input(float mouseX, float mouseY, boolean pressed, double wheel) {
        if (pressed && !lastPressed) {
            click(mouseX, mouseY);
        }
        if (wheel != 0) {
            scroll(mouseX, mouseY, wheel);
        }
        lastPressed = pressed;
    }

    public void onFrequencyList(List<FrequencyEntry> entries) {
        awaitingList = false;
        if (selected != null) {
            FrequencyEntry updated = null;
            for (FrequencyEntry entry : entries) {
                if (entry.sameIdentity(selected.typeName(), selected.security(), selected.name(), selected.owner())) {
                    updated = entry;
                    break;
                }
            }
            if (updated == null) {
                selected = null;
                network = null;
                awaitingNetwork = false;
            } else {
                selected = updated;
                if (network == null) {
                    awaitingNetwork = true;
                    MekaPdaClientState.requestNetwork(updated);
                }
            }
        }
    }

    public void onNetwork(NetworkPayload payload) {
        if (selected != null && equalsIdentity(payload.summary())) {
            network = payload;
            awaitingNetwork = false;
            netScroll = 0;
        }
    }

    private boolean equalsIdentity(NetworkSummary summary) {
        return selected != null && summary.typeName().equals(selected.typeName())
              && summary.security() == selected.security() && summary.name().equals(selected.name())
              && java.util.Objects.equals(summary.owner(), selected.owner());
    }

    private void openNetwork(FrequencyEntry entry) {
        startTransition(1);
        selected = entry;
        network = null;
        awaitingNetwork = true;
        netScroll = 0;
        if (sort == Sort.DEVICES) {
            sort = Sort.NAME;
        }
        MekaPdaClientState.requestNetwork(entry);
    }

    private void back() {
        startTransition(-1);
        selected = null;
        network = null;
        awaitingNetwork = false;
        netScroll = 0;
    }

    public void render(Canvas canvas, int mouseX, int mouseY, boolean focused, float dt) {
        tooltip = null;
        transition = Math.max(0.0f, transition - dt / 0.16f);

        drawCasing(canvas);
        drawPanel(canvas);
        drawHeader(canvas, mouseX, mouseY);
        if (selected == null) {
            drawList(canvas, mouseX, mouseY);
        } else {
            drawNetwork(canvas, mouseX, mouseY);
        }
        if (tooltip != null) {
            drawTooltip(canvas, mouseX, mouseY);
        }
        if (focused) {
            drawCursor(canvas, mouseX, mouseY);
        }
    }

    private float transitionOffset() {
        if (transition <= 0) {
            return 0;
        }
        return transitionDir * W * 0.26f * transition * transition;
    }

    private void drawCasing(Canvas canvas) {

        canvas.gradient(0, 0, SURFACE_W, SURFACE_H, 0xFF707070, 0xFF3A3A3A);
        canvas.gradient(0, 0, SURFACE_W, 2, 0xFFA2A2A2, 0xFF8C8C8C);
        canvas.gradient(0, 2, 2, SURFACE_H - 2, 0xFF8C8C8C, 0xFF4C4C4C);
        canvas.gradient(SURFACE_W - 2, 2, SURFACE_W, SURFACE_H - 2, 0xFF3A3A3A, 0xFF181818);
        canvas.gradient(0, SURFACE_H - 2, SURFACE_W, SURFACE_H, 0xFF1A1A1A, 0xFF101010);

        canvas.fill(FRAME - 3, FRAME - 3, SURFACE_W - FRAME + 3, SURFACE_H - FRAME + 3, 0xFF040404);
    }

    private void drawPanel(Canvas canvas) {
        canvas.fill(x0, y0, x0 + W, y0 + H, PdaTheme.PANEL);
        canvas.frame(x0, y0, x0 + W, y0 + H, 1, PdaTheme.EDGE);
    }

    private void drawHeader(Canvas canvas, int mouseX, int mouseY) {
        float x1 = x0 + W;
        if (selected != null) {
            float backX = x0 + MARGIN;
            drawSquareButton(canvas, backX, y0 + 4, 16, mouseX, mouseY);
            sprite(canvas, PdaTheme.ICON_BACK, backX + 2, y0 + 12, 12, PdaTheme.TEXT);
        }

        float closeX = x1 - MARGIN - 16;
        drawSquareButton(canvas, closeX, y0 + 4, 16, mouseX, mouseY);
        canvas.line(closeX + 4, y0 + 8, closeX + 12, y0 + 16, 1.4f, PdaTheme.TEXT);
        canvas.line(closeX + 12, y0 + 8, closeX + 4, y0 + 16, 1.4f, PdaTheme.TEXT);
    }

    private boolean drawSquareButton(Canvas canvas, float x, float y, float size, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, size, size);
        canvas.fill(x, y, x + size, y + size, hover ? 0x80145A1A : 0x40041406);
        canvas.frame(x, y, x + size, y + size, 1, hover ? PdaTheme.ACCENT_DIM : PdaTheme.EDGE_SOFT);
        return hover;
    }

    private void drawList(Canvas canvas, int mouseX, int mouseY) {
        float listX = x0 + MARGIN;
        float listY = y0 + 78;
        float listW = W - MARGIN * 2;
        float listH = y0 + H - 8 - listY;

        drawTabs(canvas, mouseX, mouseY);
        drawSortButton(canvas, listX, y0 + 54, mouseX, mouseY);

        List<FrequencyEntry> entries = visibleFrequencies();
        if (awaitingList && entries.isEmpty()) {
            drawCentredMessage(canvas, Component.translatable("mekapda.screen.loading").getString(), y0 + 140,
                  PdaTheme.ACCENT);
            return;
        }
        if (entries.isEmpty()) {
            String empty = switch (tab) {
                case PUBLIC -> "mekapda.screen.empty.public";
                case TRUSTED -> "mekapda.screen.empty.trusted";
                case PRIVATE -> "mekapda.screen.empty.private";
            };
            drawCentredMessage(canvas, Component.translatable(empty).getString(), y0 + 132, PdaTheme.TEXT_DIM);
            drawCentredMessage(canvas, Component.translatable("mekapda.screen.empty.hint").getString(), y0 + 148,
                  PdaTheme.TEXT_DIM);
            return;
        }

        float content = entries.size() * (ROW_H + ROW_GAP) - ROW_GAP;
        scroll = clamp(scroll, Math.max(0, content - listH));
        canvas.pushClip(listX, listY, listX + listW, listY + listH);
        canvas.push().translate(transitionOffset(), 0);
        for (int i = 0; i < entries.size(); i++) {
            float top = listY + i * (ROW_H + ROW_GAP) - (float) scroll;
            if (top + ROW_H < listY || top > listY + listH) {
                continue;
            }
            FrequencyEntry entry = entries.get(i);
            boolean hover = inside(mouseX, mouseY, listX, Math.max(listY, top), listW,
                  Math.min(ROW_H, listY + listH - top));
            drawFrequencyRow(canvas, entry, listX, top, listW, hover);
        }
        canvas.pop();
        boolean scrollable = content > listH;
        drawEdgeFade(canvas, listX, listY, listW, listH, scrollable && scroll > 0.5,
              scrollable && scroll < content - listH - 0.5);
        canvas.popClip();
        drawScrollbar(canvas, listX + listW + 3, listY, listH, content, (float) scroll);
    }

    private void drawTabs(Canvas canvas, int mouseX, int mouseY) {
        float x = x0 + MARGIN;
        float y = y0 + 28;
        float gap = 4;
        float w = (W - MARGIN * 2 - gap * 2) / 3;
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Tab current = tabs[i];
            boolean active = current == tab;
            boolean hover = inside(mouseX, mouseY, x, y, w, 18);
            canvas.fill(x, y, x + w, y + 18, active ? 0x80145A1A : hover ? 0x500A2A0E : 0x30020804);
            canvas.frame(x, y, x + w, y + 18, 1,
                  active ? PdaTheme.ACCENT_DIM : hover ? PdaTheme.EDGE : PdaTheme.EDGE_SOFT);
            sprite(canvas, PdaTheme.securityIcon(current.security), x + 8, y + 9, 14,
                  active ? PdaTheme.ACCENT : PdaTheme.TEXT);
            text(canvas, Component.translatable(current.translationKey).getString(), x + 27, y + 9, 1.0f,
                  active ? PdaTheme.TEXT : PdaTheme.TEXT_DIM);
            x += w + gap;
        }
    }

    private void drawSortButton(Canvas canvas, float x, float y, int mouseX, int mouseY) {
        boolean hover = drawSquareButton(canvas, x, y, 16, mouseX, mouseY);
        sprite(canvas, PdaTheme.ICON_SORTING, x + 1, y + 8, 14, hover ? PdaTheme.ACCENT : PdaTheme.TEXT);
        text(canvas, sort.label(), x + 22, y + 8, 1.0f, hover ? PdaTheme.TEXT : PdaTheme.TEXT_DIM);
    }

    private void drawFrequencyRow(Canvas canvas, FrequencyEntry entry, float x, float y, float w, boolean hover) {
        canvas.fill(x, y, x + w, y + ROW_H, hover ? PdaTheme.HOVER : PdaTheme.PANEL_INNER);
        canvas.frame(x, y, x + w, y + ROW_H, 1, hover ? PdaTheme.ACCENT_DIM : PdaTheme.EDGE_SOFT);

        float textX = x + 10;
        float textW = w - 10 - 70;
        text(canvas, trim(canvas, entry.name(), textW, 1.0f), textX, y + 9, 1.0f, PdaTheme.TEXT);
        String subtitle = PdaTheme.typeLabel(entry.typeName()) + "  (" + entry.ownerName() + ")";
        text(canvas, trim(canvas, subtitle, textW, 1.0f), textX, y + 20, 1.0f, PdaTheme.TEXT_DIM);

        drawStat(canvas, PdaTheme.ICON_ITEMS, PdaTheme.format(entry.itemCount()), textX, y + 30,
              entry.itemCount() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
        drawStat(canvas, PdaTheme.ICON_ENERGY, PdaTheme.format(entry.energy()), textX + 56, y + 30,
              entry.energy() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
        drawStat(canvas, PdaTheme.ICON_FLUID, PdaTheme.format(entry.fluid()), textX + 112, y + 30,
              entry.fluid() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
        drawStat(canvas, PdaTheme.ICON_CHEMICAL, PdaTheme.format(entry.chemical()), textX + 168, y + 30,
              entry.chemical() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
    }

    private List<FrequencyEntry> visibleFrequencies() {
        List<FrequencyEntry> entries = new ArrayList<>();
        for (FrequencyEntry entry : MekaPdaClientState.frequencies()) {
            if (entry.security() == tab.security) {
                entries.add(entry);
            }
        }
        entries.sort(frequencyComparator());
        return entries;
    }

    private Comparator<FrequencyEntry> frequencyComparator() {
        Comparator<FrequencyEntry> byName = Comparator
              .comparing((FrequencyEntry entry) -> entry.typeName(), String.CASE_INSENSITIVE_ORDER)
              .thenComparing(FrequencyEntry::name, String.CASE_INSENSITIVE_ORDER);
        return switch (sort) {
            case DEVICES -> Comparator.comparingInt(FrequencyEntry::deviceCount).reversed().thenComparing(byName);
            case ITEMS -> Comparator.comparingLong(FrequencyEntry::itemCount).reversed().thenComparing(byName);
            case ENERGY -> Comparator.comparingLong(FrequencyEntry::energy).reversed().thenComparing(byName);
            case FLUID -> Comparator.comparingLong(FrequencyEntry::fluid).reversed().thenComparing(byName);
            case CHEMICAL -> Comparator.comparingLong(FrequencyEntry::chemical).reversed().thenComparing(byName);
            default -> byName;
        };
    }

    private void drawNetwork(Canvas canvas, int mouseX, int mouseY) {
        NetworkSummary summary = network == null ? null : network.summary();
        float x = x0 + MARGIN;
        float w = W - MARGIN * 2;

        float identityY = y0 + 28;
        canvas.fill(x, identityY, x + w, identityY + 24, 0xB0000000);
        canvas.frame(x, identityY, x + w, identityY + 24, 1, PdaTheme.EDGE_SOFT);
        if (summary != null) {
            text(canvas, trim(canvas, summary.name(), 200, 1.0f), x + 10, identityY + 7, 1.0f, PdaTheme.TEXT);
            text(canvas, PdaTheme.typeLabel(summary.typeName()) + "  ("
                  + summary.ownerName() + ")", x + 10,
                  identityY + 17, 1.0f, PdaTheme.TEXT_DIM);
        } else {
            drawCentredMessage(canvas, Component.translatable("mekapda.screen.loading").getString(), identityY + 12,
                  PdaTheme.ACCENT);
        }

        if (summary != null) {
            float chipY = y0 + 60;
            float chipGap = 3;
            float chipW = (w - chipGap * 3) / 4;
            float chipX = x;
            drawChip(canvas, PdaTheme.ICON_ITEMS, PdaTheme.format(summary.itemCount()), chipX, chipY, chipW,
                  summary.itemCount() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
            chipX += chipW + chipGap;
            drawChip(canvas, PdaTheme.ICON_ENERGY, PdaTheme.format(summary.energy()), chipX, chipY, chipW,
                  summary.energy() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
            chipX += chipW + chipGap;
            drawChip(canvas, PdaTheme.ICON_FLUID, PdaTheme.format(summary.fluid()), chipX, chipY, chipW,
                  summary.fluid() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
            chipX += chipW + chipGap;
            drawChip(canvas, PdaTheme.ICON_CHEMICAL, PdaTheme.format(summary.chemical()), chipX, chipY, chipW,
                  summary.chemical() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
        }

        drawSortButton(canvas, x, y0 + 82, mouseX, mouseY);

        float listX = x;
        float listY = y0 + 106;
        float listW = w;
        float listH = y0 + H - 8 - listY;

        if (awaitingNetwork) {
            drawCentredMessage(canvas, Component.translatable("mekapda.screen.loading").getString(), listY + 50,
                  PdaTheme.ACCENT);
            return;
        }
        if (network == null) {
            drawCentredMessage(canvas, Component.translatable("mekapda.screen.loading").getString(), listY + 50,
                  PdaTheme.TEXT_DIM);
            return;
        }

        List<DeviceEntry> devices = sortedDevices();
        float content = devices.size() * (NET_ROW_H + NET_ROW_GAP) - NET_ROW_GAP;
        content = Math.max(0, content);
        netScroll = clamp(netScroll, Math.max(0, content - listH));

        canvas.pushClip(listX, listY, listX + listW, listY + listH);
        canvas.push().translate(transitionOffset(), 0);
        for (int i = 0; i < devices.size(); i++) {
            float top = listY + i * (NET_ROW_H + NET_ROW_GAP) - (float) netScroll;
            if (top + NET_ROW_H >= listY && top <= listY + listH) {
                DeviceEntry device = devices.get(i);
                boolean hover = inside(mouseX, mouseY, listX, Math.max(listY, top), listW,
                      Math.min(NET_ROW_H, listY + listH - top));
                float buttonX = listX + listW - 26;
                boolean buttonHover = hover && mouseX >= buttonX && mouseX < buttonX + 16
                      && mouseY >= top + 12 && mouseY < top + 28;
                float configX = listX + listW - 48;
                boolean configHover = hover && mouseX >= configX && mouseX < configX + 16
                      && mouseY >= top + 12 && mouseY < top + 28;
                drawDeviceRow(canvas, device, listX, top, listW, hover, buttonHover, configHover);
                if (configHover) {
                    tooltip = Component.translatable("mekapda.screen.config").getString();
                } else if (buttonHover) {
                    tooltip = Component.translatable("mekapda.screen.highlight").getString();
                }
            }
        }
        canvas.pop();
        boolean scrollable = content > listH;
        drawEdgeFade(canvas, listX, listY, listW, listH, scrollable && netScroll > 0.5,
              scrollable && netScroll < content - listH - 0.5);
        canvas.popClip();
        drawScrollbar(canvas, listX + listW + 3, listY, listH, content, (float) netScroll);
    }

    private List<DeviceEntry> sortedDevices() {
        List<DeviceEntry> devices = new ArrayList<>(network == null ? List.of() : network.devices());
        devices.sort(deviceComparator());
        return devices;
    }

    private Comparator<DeviceEntry> deviceComparator() {
        Comparator<DeviceEntry> byName = Comparator.comparing(DeviceEntry::name, String.CASE_INSENSITIVE_ORDER);
        return switch (sort) {
            case ITEMS -> Comparator.comparingLong(DeviceEntry::itemCount).reversed().thenComparing(byName);
            case ENERGY -> Comparator.comparingLong(DeviceEntry::energy).reversed().thenComparing(byName);
            case FLUID -> Comparator.comparingLong(DeviceEntry::fluid).reversed().thenComparing(byName);
            case CHEMICAL -> Comparator.comparingLong(DeviceEntry::chemical).reversed().thenComparing(byName);
            default -> byName;
        };
    }

    private void drawDeviceRow(Canvas canvas, DeviceEntry device, float x, float y, float w, boolean hover,
                               boolean buttonHover, boolean configHover) {
        canvas.fill(x, y, x + w, y + NET_ROW_H, hover ? PdaTheme.HOVER : PdaTheme.PANEL_INNER);
        canvas.frame(x, y, x + w, y + NET_ROW_H, 1, hover ? PdaTheme.ACCENT_DIM : PdaTheme.EDGE_SOFT);

        float textX = x + 10;
        text(canvas, trim(canvas, device.name(), w - 220, 1.0f), textX, y + 9, 1.0f, PdaTheme.TEXT);
        text(canvas, device.dimension().getPath() + "  " + device.pos().getX() + ", " + device.pos().getY()
              + ", " + device.pos().getZ(), textX, y + 20, 1.0f, PdaTheme.TEXT_DIM);

        if (device.driveSlots() > 0) {
            drawStat(canvas, PdaTheme.ICON_ITEMS, device.drives() + "/" + device.driveSlots(), textX, y + 30,
                  PdaTheme.TEXT);
        } else {
            drawStat(canvas, PdaTheme.ICON_ITEMS, PdaTheme.format(device.itemCount()), textX, y + 30,
                  device.itemCount() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
            drawStat(canvas, PdaTheme.ICON_ENERGY, PdaTheme.format(device.energy()), textX + 56, y + 30,
                  device.energy() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
            drawStat(canvas, PdaTheme.ICON_FLUID, PdaTheme.format(device.fluid()), textX + 112, y + 30,
                  device.fluid() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
            drawStat(canvas, PdaTheme.ICON_CHEMICAL, PdaTheme.format(device.chemical()), textX + 168, y + 30,
                  device.chemical() > 0 ? PdaTheme.TEXT : PdaTheme.TEXT_MUTED);
        }

        float cx = x + w - 48;
        float cy = y + 12;
        canvas.fill(cx, cy, cx + 16, cy + 16, configHover ? 0x80145A1A : 0x40041406);
        canvas.frame(cx, cy, cx + 16, cy + 16, 1, configHover ? PdaTheme.ACCENT : PdaTheme.EDGE_SOFT);
        int ink = configHover ? PdaTheme.ACCENT : PdaTheme.TEXT;
        sprite(canvas, PdaTheme.ICON_CONFIG, cx + 2, cy + 8, 12, ink);

        float bx = x + w - 26;
        float by = y + 12;
        canvas.fill(bx, by, bx + 16, by + 16, buttonHover ? 0x80145A1A : 0x40041406);
        canvas.frame(bx, by, bx + 16, by + 16, 1, buttonHover ? PdaTheme.ACCENT : PdaTheme.EDGE_SOFT);
        textCentred(canvas, "?", bx + 8, by + 8, 1.0f, buttonHover ? PdaTheme.ACCENT : PdaTheme.TEXT);
    }

    private void drawStat(Canvas canvas, ResourceLocation icon, String text, float x, float centerY, int color) {
        sprite(canvas, icon, x, centerY, 9, PdaTheme.TEXT);
        text(canvas, text, x + 12, centerY, 1.0f, color);
    }

    private void drawChip(Canvas canvas, ResourceLocation icon, String text, float x, float y, float w, int color) {
        canvas.fill(x, y, x + w, y + 14, 0x50041A08);
        canvas.frame(x, y, x + w, y + 14, 1, PdaTheme.EDGE_SOFT);
        sprite(canvas, icon, x + 5, y + 7, 9, PdaTheme.TEXT);
        text(canvas, text, x + 17, y + 7, 1.0f, color);
    }

    private void drawEdgeFade(Canvas canvas, float x, float y, float w, float h, boolean top, boolean bottom) {
        float edge = 22.0f;
        if (top) {
            canvas.gradient(x, y, x + w, y + edge, PdaTheme.EDGE_FADE, 0x00020602);
        }
        if (bottom) {
            canvas.gradient(x, y + h - edge, x + w, y + h, 0x00020602, PdaTheme.EDGE_FADE);
        }
    }

    private void drawScrollbar(Canvas canvas, float x, float y, float h, float content, float scroll) {
        if (content <= h) {
            return;
        }
        canvas.fill(x, y, x + 3, y + h, 0x40041A08);
        float size = Math.max(10, h * h / content);
        float at = y + (h - size) * scroll / (content - h);
        canvas.fill(x, at, x + 3, at + size, PdaTheme.ACCENT_DIM);
    }

    private void drawCentredMessage(Canvas canvas, String text, float y, int color) {
        textCentred(canvas, text, x0 + W * 0.5f, y, 1.0f, color);
    }

    private void drawTooltip(Canvas canvas, int mouseX, int mouseY) {
        float width = canvas.width(tooltip) + 12;
        float height = 16;
        float x = Math.min(mouseX + 12, x0 + W - width - 4);
        float y = Math.max(y0 + 2, Math.min(mouseY + 12, y0 + H - height - 4));
        canvas.fill(x, y, x + width, y + height, 0xF0000000);
        canvas.frame(x, y, x + width, y + height, 1, PdaTheme.ACCENT_DIM);
        text(canvas, tooltip, x + 5, y + 8, 1.0f, PdaTheme.TEXT);
    }

    private void drawCursor(Canvas canvas, int mouseX, int mouseY) {
        canvas.triangle(mouseX - 1, mouseY - 2, mouseX - 1, mouseY + 13, mouseX + 10, mouseY + 10, 0xFF000000);
        canvas.triangle(mouseX, mouseY, mouseX, mouseY + 10, mouseX + 7.5f, mouseY + 8, PdaTheme.ACCENT);
    }

    private void text(Canvas canvas, String text, float x, float centerY, float scale, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        canvas.push().translate(x, centerY - BASELINE * scale).scale(scale);
        canvas.text(text, 0, 0, color, false);
        canvas.pop();
    }

    private void textCentred(Canvas canvas, String text, float centerX, float centerY, float scale, int color) {

        text(canvas, text, centerX - canvas.width(text) * scale * 0.5f + 0.5f * scale, centerY, scale, color);
    }

    private void sprite(Canvas canvas, ResourceLocation icon, float x, float centerY, float size, int color) {
        float top = centerY - size * 0.5f;
        canvas.sprite(icon, x, top, x + size, top + size, 0, 0, 1, 1, color);
    }

    private String trim(Canvas canvas, String text, float maxWidth, float scale) {
        if (text == null) {
            return "";
        }
        if (canvas.width(text) * scale <= maxWidth) {
            return text;
        }
        String suffix = "..";
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i) + suffix;
            if (canvas.width(candidate) * scale <= maxWidth) {
                return candidate;
            }
        }
        return suffix;
    }

    private static boolean inside(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static double clamp(double value, double max) {
        return Math.max(0, Math.min(value, max));
    }

    private void click(double mouseX, double mouseY) {
        float x1 = x0 + W;
        if (inside(mouseX, mouseY, x1 - MARGIN - 16, y0 + 4, 16, 16)) {
            PdaClient.lower();
            return;
        }
        if (selected != null) {
            if (inside(mouseX, mouseY, x0 + MARGIN, y0 + 4, 16, 16)) {
                back();
                return;
            }
            if (inside(mouseX, mouseY, x0 + MARGIN, y0 + 82, 16, 16)) {
                sort = sort.nextNetwork();
                return;
            }
            if (mouseX >= x0 + MARGIN && mouseX < x0 + W - MARGIN && mouseY >= y0 + 106
                  && mouseY < y0 + H - 8) {
                DeviceEntry device = deviceAt(mouseX, mouseY);
                if (device != null) {
                    float highlightX = x0 + W - MARGIN - 26;
                    float configX = x0 + W - MARGIN - 48;
                    if (mouseX >= highlightX && mouseX < highlightX + 16) {
                        PdaClient.toggleHighlight(device);
                    } else if (mouseX >= configX && mouseX < configX + 16) {
                        PdaClient.openDeviceGui(device);
                    } else {
                        copyCoordinates(device);
                    }
                }
            }
            return;
        }

        float tabX = x0 + MARGIN;
        float tabY = y0 + 28;
        float gap = 4;
        float tabW = (W - MARGIN * 2 - gap * 2) / 3;
        for (Tab candidate : Tab.values()) {
            if (inside(mouseX, mouseY, tabX, tabY, tabW, 18)) {
                if (candidate != tab) {
                    startTransition(candidate.ordinal() > tab.ordinal() ? 1 : -1);
                }
                tab = candidate;
                scroll = 0;
                return;
            }
            tabX += tabW + gap;
        }
        if (inside(mouseX, mouseY, x0 + MARGIN, y0 + 54, 16, 16)) {
            sort = sort.nextList();
            return;
        }
        if (mouseY >= y0 + 78 && mouseY < y0 + H - 8) {
            FrequencyEntry entry = frequencyAt(mouseX, mouseY);
            if (entry != null) {
                openNetwork(entry);
            }
        }
    }

    private void scroll(double mouseX, double mouseY, double scrollY) {
        float listY = selected == null ? y0 + 78 : y0 + 106;
        float listH = y0 + H - 8 - listY;
        if (mouseX >= x0 + MARGIN && mouseX < x0 + W - MARGIN && mouseY >= listY && mouseY < listY + listH) {
            if (selected == null) {
                scroll -= scrollY * 24;
                scroll = clamp(scroll, listContentHeight());
            } else {
                netScroll -= scrollY * 20;
                netScroll = clamp(netScroll, networkContentHeight());
            }
        }
    }

    private double networkContentHeight() {
        if (network == null) {
            return 0;
        }
        float content = network.devices().size() * (NET_ROW_H + NET_ROW_GAP) - NET_ROW_GAP;
        float listH = y0 + H - 8 - (y0 + 106);
        return Math.max(0, content - listH);
    }

    private double listContentHeight() {
        float listH = y0 + H - 8 - (y0 + 78);
        float content = visibleFrequencies().size() * (ROW_H + ROW_GAP) - ROW_GAP;
        return Math.max(0, content - listH);
    }

    private FrequencyEntry frequencyAt(double mouseX, double mouseY) {
        List<FrequencyEntry> entries = visibleFrequencies();
        float listY = y0 + 78;
        float listH = y0 + H - 8 - listY;
        if (mouseY < listY || mouseY >= listY + listH) {
            return null;
        }
        for (int i = 0; i < entries.size(); i++) {
            float top = listY + i * (ROW_H + ROW_GAP) - (float) scroll;
            if (mouseY >= top && mouseY < top + ROW_H) {
                return entries.get(i);
            }
        }
        return null;
    }

    private DeviceEntry deviceAt(double mouseX, double mouseY) {
        if (network == null) {
            return null;
        }
        List<DeviceEntry> devices = sortedDevices();
        float listY = y0 + 106;
        float listH = y0 + H - 8 - listY;
        if (mouseY < listY || mouseY >= listY + listH) {
            return null;
        }
        for (int i = 0; i < devices.size(); i++) {
            float top = listY + i * (NET_ROW_H + NET_ROW_GAP) - (float) netScroll;
            if (mouseY >= top && mouseY < top + NET_ROW_H) {
                return devices.get(i);
            }
        }
        return null;
    }

    private void copyCoordinates(DeviceEntry device) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(device.dimension() + " " + device.pos().getX() + " "
                  + device.pos().getY() + " " + device.pos().getZ());
        }
    }

    private enum Tab {
        PUBLIC(0, "mekapda.screen.public"),
        TRUSTED(2, "mekapda.screen.trusted"),
        PRIVATE(1, "mekapda.screen.private");

        private final int security;
        private final String translationKey;

        Tab(int security, String translationKey) {
            this.security = security;
            this.translationKey = translationKey;
        }
    }

    private enum Sort {
        NAME,
        DEVICES,
        ITEMS,
        ENERGY,
        FLUID,
        CHEMICAL;

        private static final Sort[] LIST = {NAME, DEVICES, ITEMS, ENERGY, FLUID, CHEMICAL};
        private static final Sort[] NETWORK = {NAME, ITEMS, ENERGY, FLUID, CHEMICAL};

        public Sort nextList() {
            for (int i = 0; i < LIST.length; i++) {
                if (LIST[i] == this) {
                    return LIST[(i + 1) % LIST.length];
                }
            }
            return NAME;
        }

        public Sort nextNetwork() {
            for (int i = 0; i < NETWORK.length; i++) {
                if (NETWORK[i] == this) {
                    return NETWORK[(i + 1) % NETWORK.length];
                }
            }
            return NAME;
        }

        public String label() {
            return Component.translatable("mekapda.screen.sort." + name().toLowerCase(java.util.Locale.ROOT)).getString();
        }
    }

}
