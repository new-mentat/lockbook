use eframe::egui;

use crate::settings::ThemeMode;
use crate::theme;

impl super::SettingsModal {
    pub fn show_appearance_tab(&mut self, ui: &mut egui::Ui) {
        ui.heading("Theme Mode:");
        ui.separator();
        ui.add_space(12.0);

        ui.horizontal(|ui| {
            let s = &mut self.settings.write().unwrap();

            for (mode, name) in vec![
                (ThemeMode::System, "System"),
                (ThemeMode::Dark, "Dark"),
                (ThemeMode::Light, "Light"),
            ] {
                if ui.selectable_value(&mut s.theme_mode, mode, name).clicked() {
                    theme::apply_settings(s, ui.ctx());
                }
            }
        });
    }
}
