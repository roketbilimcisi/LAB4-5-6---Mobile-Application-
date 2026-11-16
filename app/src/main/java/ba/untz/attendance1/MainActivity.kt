package ba.untz.attendance1

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ba.untz.attendance1.ui.theme.Attendance1Theme
import androidx.compose.foundation.rememberScrollState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Attendance1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    ProfessorScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
fun AttendanceSummary(checkIns: List<CheckIn>, modifier: Modifier = Modifier) {
    val onTime = checkIns.count { it.status == CheckInStatus.OnTime }
    val late = checkIns.count { it.status is CheckInStatus.Late }
    val absent = checkIns.count { it.status == CheckInStatus.Absent }
    Text("On time: $onTime · Late: $late · Absent: $absent", modifier)
}

@Preview(showBackground = true)
@Composable
private fun AttendanceSummaryPreview() { Attendance1Theme { AttendanceSummary(sampleCheckIns) } }

// Prediction: the MIDDLE preview shows On time: 4 · Late: 2 · Absent: 0.
@Preview(name = "Middle summary", showBackground = true)
@Composable
private fun MiddleSummaryPreview() { Attendance1Theme { AttendanceSummary(sampleCheckIns.filter { it.window == Window.MIDDLE }) } }

@Composable
fun Avatar(initials: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun StatusBadge(status: CheckInStatus, modifier: Modifier = Modifier) {
    Text(
        status.label(),
        modifier = modifier.clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = status.color(),
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
fun CheckInStatus.color(): Color = when (this) {
    CheckInStatus.OnTime -> MaterialTheme.colorScheme.primary
    is CheckInStatus.Late -> MaterialTheme.colorScheme.error
    CheckInStatus.Absent -> MaterialTheme.colorScheme.outline
}

fun CheckIn.timeText(): String = if (status == CheckInStatus.Absent) "--" else
    "%02d:%02d".format(minutes / 60, minutes % 60)

@Composable
fun CheckInRow(checkIn: CheckIn, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            Avatar(checkIn.student.initials())
            if (checkIn.status is CheckInStatus.Late) {
                Box(Modifier.align(Alignment.BottomEnd).size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(checkIn.student.name, style = MaterialTheme.typography.titleMedium)
            StatusBadge(checkIn.status)
        }
        Text(checkIn.timeText(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun CheckInRowPreview() { Attendance1Theme { Surface { CheckInRow(sampleCheckIns[1]) } } }

@Composable
fun WindowControls(openWindow: Window?, onOpen: (Window) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Text("Open: ${openWindow?.label ?: "none"}", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Window.entries.forEach { window ->
                Button(onClick = { onOpen(window) }, enabled = openWindow != window) {
                    Text(window.label.replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

@Preview(name = "Middle is open", showBackground = true)
@Composable
private fun WindowControlsPreview() { Attendance1Theme { WindowControls(Window.MIDDLE, onOpen = {}) } }

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text("Search student") },
        placeholder = { Text("e.g. Amir") },
        singleLine = true
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchFieldPreview() {
    Attendance1Theme { SearchField(query = "ami", onQueryChange = {}) }
}

@Composable
fun LateOnlySwitch(onlyLate: Boolean, onOnlyLateChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().toggleable(
            value = onlyLate,
            role = Role.Switch,
            onValueChange = onOnlyLateChange
        ).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Only late", Modifier.weight(1f))
        Switch(checked = onlyLate, onCheckedChange = null)
    }
}

@Preview(name = "Late only enabled", showBackground = true)
@Composable
private fun LateOnlySwitchEnabledPreview() {
    Attendance1Theme { LateOnlySwitch(onlyLate = true, onOnlyLateChange = {}) }
}

@Preview(name = "Late only disabled", showBackground = true)
@Composable
private fun LateOnlySwitchDisabledPreview() {
    Attendance1Theme { LateOnlySwitch(onlyLate = false, onOnlyLateChange = {}) }
}

enum class StatusKind(val label: String) {
    ON_TIME("On time"),
    LATE("Late"),
    ABSENT("Absent")
}

fun Window.startMinutes(): Int = when (this) {
    Window.START -> 480
    Window.MIDDLE -> 600
    Window.END -> 690
}

fun StatusKind.toStatus(lateMinutes: Int): CheckInStatus = when (this) {
    StatusKind.ON_TIME -> CheckInStatus.OnTime
    StatusKind.LATE -> CheckInStatus.Late(lateMinutes)
    StatusKind.ABSENT -> CheckInStatus.Absent
}

// Prediction: If each radio option owns its own remember { mutableStateOf(false) }, each option can become selected independently, so multiple statuses may appear selected at once.
@Composable
fun StatusPicker(selected: StatusKind, onSelect: (StatusKind) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.selectableGroup()) {
        StatusKind.entries.forEach { status ->
            Row(
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = selected == status,
                    role = Role.RadioButton,
                    onClick = { onSelect(status) }
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == status, onClick = null)
                Text(status.label)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusPickerPreview() {
    Attendance1Theme { StatusPicker(selected = StatusKind.LATE, onSelect = {}) }
}

// Prediction: 5-minute intervals from 5 to 60 require 10 interior steps. Setting steps = 12 creates 14 total selectable values; the spacing is 55/13 minutes, so the intermediate values are not all whole numbers.
@Composable
fun LateMinutesSlider(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(if (enabled) "Late: $minutes min" else "Late: —")
        Slider(
            value = minutes.toFloat(),
            onValueChange = { onMinutesChange(it.roundToInt()) },
            enabled = enabled,
            valueRange = 5f..60f,
            steps = 10
        )
    }
}

@Preview(name = "Late slider enabled", showBackground = true)
@Composable
private fun LateMinutesSliderEnabledPreview() {
    Attendance1Theme {
        LateMinutesSlider(minutes = 15, onMinutesChange = {}, enabled = true)
    }
}

@Preview(name = "Late slider disabled", showBackground = true)
@Composable
private fun LateMinutesSliderDisabledPreview() {
    Attendance1Theme {
        LateMinutesSlider(minutes = 15, onMinutesChange = {}, enabled = false)
    }
}

@Composable
fun ManualCheckInForm(
    window: Window,
    existing: List<CheckIn>,
    onSave: (CheckIn) -> Unit,
    modifier: Modifier = Modifier
) {
    var index by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var statusKind by rememberSaveable { mutableStateOf(StatusKind.ON_TIME) }
    var lateMinutes by rememberSaveable { mutableStateOf(5) }
    var confirmed by rememberSaveable { mutableStateOf(false) }

    val duplicateIndex = existing.any { it.window == window && it.student.id == index }
    val validIndex = Regex("IB\\d{3}").matches(index) && !duplicateIndex
    val validName = name.trim().contains(" ")
    val canSave = validIndex && validName && confirmed

    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Manual check-in · ${window.label}", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = index,
            onValueChange = { index = it.uppercase() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Index") },
            isError = index.isNotEmpty() && !validIndex,
            supportingText = {
                Text(if (duplicateIndex) "Already checked in for this window" else "Format: IB + 3 digits")
            },
            singleLine = true
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("First and last name") },
            isError = name.isNotEmpty() && !validName,
            supportingText = { Text("e.g. Tarik Hodžić") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true
        )
        StatusPicker(selected = statusKind, onSelect = { statusKind = it })
        LateMinutesSlider(
            minutes = lateMinutes,
            onMinutesChange = { lateMinutes = it },
            enabled = statusKind == StatusKind.LATE
        )
        Row(
            modifier = Modifier.fillMaxWidth().toggleable(
                value = confirmed,
                role = Role.Checkbox,
                onValueChange = { confirmed = it }
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = confirmed, onCheckedChange = null)
            Text("Confirmed in person")
        }
        Button(
            onClick = {
                val student = Student(index, name.trim())
                val status = statusKind.toStatus(lateMinutes)
                onSave(
                    CheckIn(
                        student = student,
                        window = window,
                        minutes = window.startMinutes() + if (statusKind == StatusKind.LATE) lateMinutes else 0,
                        status = status
                    )
                )
                index = ""
                name = ""
                statusKind = StatusKind.ON_TIME
                lateMinutes = 5
                confirmed = false
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add check-in")
        }
    }
}

@Composable
fun CheckInCard(checkIn: CheckIn, allCheckIns: List<CheckIn>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val sameStudent = allCheckIns.filter { it.student == checkIn.student }
    Column(modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        CheckInRow(checkIn)
        if (expanded) {
            Column(Modifier.padding(start = 64.dp, bottom = 8.dp)) {
                sameStudent.forEach { item ->
                    Text("${item.window.label}: ${item.status.label()}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ProfessorScreen(modifier: Modifier = Modifier) {
    var openWindow by rememberSaveable { mutableStateOf<Window?>(null) }
    // Prediction: On rotation, rememberSaveable values such as openWindow, query, onlyLate, and the form draft are restored, while remember values such as checkIns and card expansion reset. Saving checkIns with rememberSaveable fails because its List<CheckIn> contents are not Bundle-saveable without a custom Saver or Parcelable implementation.
    var checkIns by remember { mutableStateOf(sampleCheckIns) }
    var query by rememberSaveable { mutableStateOf("") }
    var onlyLate by rememberSaveable { mutableStateOf(false) }
    val current = checkIns.filter { it.window == openWindow }
    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        WindowControls(openWindow, onOpen = { openWindow = it })
        if (openWindow == null) {
            Text("Open a window to see check-ins", Modifier.padding(16.dp))
        } else {
            // The summary reports the full window; the filter only changes the displayed cards.
            AttendanceSummary(current, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            // If onQueryChange = {} is passed here, typing cannot update query, so the field stays unchanged.
            SearchField(query = query, onQueryChange = { query = it }, modifier = Modifier.padding(horizontal = 16.dp))
            LateOnlySwitch(onlyLate = onlyLate, onOnlyLateChange = { onlyLate = it })
            // Prediction: In the middle window, searching "s" with Only late off shows Faris Ćosić and Selma Lugavić; Hedija Šišić is excluded because "Š" is not the same letter as unaccented "s". With Only late on, Faris and Selma remain because both matching check-ins are late.
            val visible = current.filter {
                it.student.name.contains(query.trim(), ignoreCase = true) &&
                    (!onlyLate || it.status is CheckInStatus.Late)
            }
            if (visible.isEmpty()) {
                Text("No check-ins match the filter", Modifier.padding(16.dp))
            } else {
                visible.forEach { checkIn ->
                    key(checkIn.student.id, checkIn.window) {
                        CheckInCard(checkIn, allCheckIns = checkIns)
                    }
                }
            }
            // Predict: checkIns = checkIns + it creates a new List value that Compose observes; MutableList.add(it) mutates in place and may not trigger recomposition.
            ManualCheckInForm(
                window = openWindow!!,
                existing = checkIns,
                onSave = { checkIns = checkIns + it }
            )
        }
    }
}
