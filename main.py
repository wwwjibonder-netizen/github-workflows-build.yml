import requests
from kivy.app import App
from kivy.uix.anchorlayout import AnchorLayout
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.textinput import TextInput

SERVER_URL = "http://127.0.0"


class WorkerAttendanceApp(App):

    def build(self):
        self.title = "হাজিরা অ্যাপ"

        # ১. মূল লেআউট (এটি পুরো স্ক্রিনকে মাঝখানে ধরে রাখবে)
        root_layout = AnchorLayout(anchor_x="center", anchor_y="center")

        # ২. ভেতরের ছোট অ্যাপ বক্স (এটি পুরো স্ক্রিনের ৮০% চওড়া এবং ৫০% লম্বা হবে)
        app_box = BoxLayout(
            orientation="vertical",
            padding=20,
            spacing=15,
            size_hint=(0.8, 0.5),  # সাইজ ছোট করার মূল কোড
        )

        # ৩. স্ট্যাটাস লেবেল
        self.status_label = Label(
            text="আইডি লিখে নিচের বাটনে চাপুন",
            font_size="18sp",
            bold=True,
            halign="center",
            valign="middle",
            size_hint=(1, 0.3),
        )
        self.status_label.bind(size=self.status_label.setter("text_size"))
        app_box.add_widget(self.status_label)

        # ৪. শ্রমিকের আইডি ইনপুট বক্স
        self.worker_id_input = TextInput(
            hint_text="এখানে শ্রমিকের আইডি লিখুন",
            font_size="16sp",
            multiline=False,
            size_hint=(1, 0.25),
            input_filter="int",
        )
        app_box.add_widget(self.worker_id_input)

        # ৫. ফিঙ্গারপ্রিন্ট বাটন
        fingerprint_btn = Button(
            text="👍 আঙুলের ছাপ দিন",
            font_size="18sp",
            bold=True,
            background_color=(0.1, 0.6, 0.3, 1),
            size_hint=(1, 0.45),
        )
        fingerprint_btn.bind(on_press=self.scan_fingerprint_and_send)
        app_box.add_widget(fingerprint_btn)

        # ছোট বক্সটিকে মূল স্ক্রিনের মাঝখানে যুক্ত করা
        root_layout.add_widget(app_box)
        return root_layout

    def scan_fingerprint_and_send(self, instance):
        worker_id = self.worker_id_input.text.strip()

        if not worker_id:
            self.status_label.text = "⚠️ প্রথমে আইডি লিখুন!"
            return

        self.status_label.text = "🔄 যাচাই করা হচ্ছে..."

        current_lat = 23.8103
        current_lon = 90.4125

        payload = {
            "worker_id": worker_id,
            "latitude": current_lat,
            "longitude": current_lon,
        }

        try:
            response = requests.post(SERVER_URL, json=payload, timeout=5)
            result = response.json()

            if response.status_code == 200:
                self.status_label.text = f"✅ {result['message']}"
                self.worker_id_input.text = ""
            else:
                self.status_label.text = f"❌ {result['message']}"

        except requests.exceptions.ConnectionError:
            self.status_label.text = "⚠️ সার্ভার বন্ধ আছে!"
        except Exception as e:
            self.status_label.text = f"⚠️ ভুল: {str(e)}"


if __name__ == "__main__":
    WorkerAttendanceApp().run()
