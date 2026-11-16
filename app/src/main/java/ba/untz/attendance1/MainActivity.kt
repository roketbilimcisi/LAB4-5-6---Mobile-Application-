package ba.untz.attendance1

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                visible.forEach { CheckInCard(it, allCheckIns = checkIns) }
            }
        }
    }
}
