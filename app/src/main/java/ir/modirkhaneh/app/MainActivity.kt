package ir.modirkhaneh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.modirkhaneh.app.data.*
import ir.modirkhaneh.app.ai.BuildingAssistant
import ir.modirkhaneh.app.ui.theme.ModirKhanehTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = UnitRepository(AppDatabase.get(this))
        setContent { ModirKhanehTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { HomeScreen(repository) } } }
    }
}

private fun money(value: Long): String = NumberFormat.getNumberInstance(Locale("fa", "IR")).format(value) + " ریال"
private fun monthKey(year: String, month: String): Int? {
    val y = year.toIntOrNull() ?: return null
    val m = month.toIntOrNull() ?: return null
    if (y !in 1300..1600 || m !in 1..12) return null
    return y * 12 + m
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(repository: UnitRepository) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var units by remember { mutableStateOf(emptyList<BuildingUnitEntity>()) }
    var charges by remember { mutableStateOf(emptyList<ChargePeriodEntity>()) }
    var payments by remember { mutableStateOf(emptyList<PaymentEntity>()) }
    var expenses by remember { mutableStateOf(emptyList<ExpenseEntity>()) }
    var tab by remember { mutableStateOf("خانه") }
    var unitNo by remember { mutableStateOf("") }; var resident by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }; var people by remember { mutableStateOf("1") }
    var startYear by remember { mutableStateOf("1405") }; var startMonth by remember { mutableStateOf("1") }
    var endYear by remember { mutableStateOf("1405") }; var endMonth by remember { mutableStateOf("12") }
    var chargeAmount by remember { mutableStateOf("") }
    var payUnit by remember { mutableStateOf("") }; var payYear by remember { mutableStateOf("1405") }
    var payMonth by remember { mutableStateOf("1") }; var payAmount by remember { mutableStateOf("") }
    var expenseTitle by remember { mutableStateOf("") }; var expenseCategory by remember { mutableStateOf("عمومی") }
    var expenseAmount by remember { mutableStateOf("") }; var expenseYear by remember { mutableStateOf("1405") }; var expenseMonth by remember { mutableStateOf("1") }
    LaunchedEffect(repository) {
        launch { repository.observeUnits().collect { units = it } }
        launch { repository.observeCharges().collect { charges = it } }
        launch { repository.observePayments().collect { payments = it } }
        launch { repository.observeExpenses().collect { expenses = it } }
    }
    fun reportError(t: Throwable) { scope.launch { snackbar.showSnackbar(t.message ?: "عملیات انجام نشد") } }
    Scaffold(topBar = { TopAppBar(title = { Text("مدیرخانه") }, subtitle = { Text("مدیریت ساختمان؛ ذخیره‌سازی محلی") }) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("خانه", "ساکنین", "شارژ", "پرداخت", "هزینه‌ها", "گزارش‌ها", "دستیار").forEach { label ->
                    FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                "ساکنین" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ثبت و فهرست واحدها", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(unitNo, { unitNo = it }, label = { Text("شماره واحد") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(resident, { resident = it }, label = { Text("نام و نام خانوادگی ساکن") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(15) }, label = { Text("شماره تماس") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(people, { people = it.filter(Char::isDigit).take(2) }, label = { Text("تعداد نفرات") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scope.launch { try { require(unitNo.isNotBlank()) { "شماره واحد الزامی است" }; repository.addUnit(BuildingUnitEntity(number = unitNo.trim(), occupantName = resident.trim(), phone = phone, peopleCount = people.toIntOrNull()?.coerceAtLeast(1) ?: 1)); unitNo=""; resident=""; phone=""; people="1"; snackbar.showSnackbar("واحد با موفقیت ثبت شد") } catch(t:Throwable) { reportError(t) } } }, modifier = Modifier.fillMaxWidth()) { Text("ثبت واحد") }
                    units.forEach { ListItem(headlineContent = { Text("واحد ${it.number} · ${it.occupantName.ifBlank { "بدون نام" }}") }, supportingContent = { Text("تماس: ${it.phone.ifBlank { "ثبت نشده" }} · نفرات: ${it.peopleCount}") }) }
                }
                "شارژ" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تعریف بازه شارژ", style = MaterialTheme.typography.titleMedium)
                    YearMonthFields("از سال", startYear, { startYear=it }, "ماه", startMonth, { startMonth=it })
                    YearMonthFields("تا سال", endYear, { endYear=it }, "ماه", endMonth, { endMonth=it })
                    MoneyField(chargeAmount, { chargeAmount=it }, "مبلغ شارژ ماهانه (ریال)")
                    Button(onClick = { scope.launch { try { val s=monthKey(startYear,startMonth) ?: error("تاریخ شروع نامعتبر است"); val e=monthKey(endYear,endMonth) ?: error("تاریخ پایان نامعتبر است"); repository.addCharge(ChargePeriodEntity(startMonthKey=s,endMonthKey=e,amountRial=chargeAmount.toLongOrNull() ?: 0)); chargeAmount=""; snackbar.showSnackbar("بازه شارژ ثبت شد") } catch(t:Throwable) { reportError(t) } } }, modifier = Modifier.fillMaxWidth()) { Text("ثبت بازه شارژ") }
                    charges.forEach { ListItem(headlineContent = { Text("${it.startMonthKey} تا ${it.endMonthKey}") }, supportingContent = { Text("مبلغ هر ماه: ${money(it.amountRial)}") }) }
                }
                "پرداخت" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ثبت پرداخت شارژ", style = MaterialTheme.typography.titleMedium)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded=expanded, onExpandedChange={expanded=!expanded}) {
                        OutlinedTextField(value=units.firstOrNull{it.id==payUnit}?.let{"واحد ${it.number}"} ?: "انتخاب واحد", onValueChange={}, readOnly=true, label={Text("واحد")}, modifier=Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { units.forEach { u -> DropdownMenuItem(text={Text("واحد ${u.number} · ${u.occupantName}")},onClick={payUnit=u.id;expanded=false}) } }
                    }
                    YearMonthFields("سال", payYear, {payYear=it}, "ماه", payMonth, {payMonth=it})
                    MoneyField(payAmount,{payAmount=it},"مبلغ پرداخت (ریال)")
                    Button(onClick={scope.launch { try { val key=monthKey(payYear,payMonth) ?: error("ماه یا سال نامعتبر است"); require(payUnit.isNotBlank()){ "ابتدا واحد را انتخاب کنید" }; repository.addPayment(payUnit,key,payAmount.toLongOrNull() ?: 0); payAmount=""; snackbar.showSnackbar("پرداخت ثبت شد") } catch(t:Throwable){reportError(t)} }},modifier=Modifier.fillMaxWidth()){Text("ثبت پرداخت")}
                    payments.take(30).forEach { p -> val u=units.firstOrNull{it.id==p.unitId}; ListItem(headlineContent={Text("واحد ${u?.number ?: "حذف‌شده"} · ${money(p.amountRial)}")}, supportingContent={Text("کد ماه: ${p.monthKey}")}) }
                }
                "هزینه‌ها" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ثبت هزینه ساختمان", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(expenseTitle,{expenseTitle=it},label={Text("شرح هزینه")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(expenseCategory,{expenseCategory=it},label={Text("دسته‌بندی")},modifier=Modifier.fillMaxWidth())
                    MoneyField(expenseAmount,{expenseAmount=it},"مبلغ هزینه (ریال)")
                    YearMonthFields("سال",expenseYear,{expenseYear=it},"ماه",expenseMonth,{expenseMonth=it})
                    Button(onClick={scope.launch { try { val key=monthKey(expenseYear,expenseMonth) ?: error("ماه یا سال نامعتبر است"); repository.addExpense(ExpenseEntity(title=expenseTitle.trim(),category=expenseCategory.trim(),amountRial=expenseAmount.toLongOrNull() ?: 0,monthKey=key)); expenseTitle="";expenseAmount="";snackbar.showSnackbar("هزینه ثبت شد") } catch(t:Throwable){reportError(t)} }},modifier=Modifier.fillMaxWidth()){Text("ثبت هزینه")}
                    expenses.forEach { ListItem(headlineContent={Text(it.title)},supportingContent={Text("${it.category} · ${money(it.amountRial)} · ماه ${it.monthKey}")}) }
                }
                "گزارش‌ها" -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("گزارش مالی ثبت‌شده",style=MaterialTheme.typography.titleMedium)
                    SummaryCard("تعداد واحدها",units.size.toString()); SummaryCard("تعداد پرداخت‌ها",payments.size.toString()); SummaryCard("جمع پرداخت‌ها",money(payments.sumOf{it.amountRial})); SummaryCard("جمع هزینه‌ها",money(expenses.sumOf{it.amountRial})); SummaryCard("بازه‌های شارژ",charges.size.toString())
                    Text("این گزارش بر اساس داده‌های ثبت‌شده در همین دستگاه است.",style=MaterialTheme.typography.bodySmall)
                }
                "دستیار" -> BuildingAssistantScreen(
                    snapshot = BuildingAssistant.Snapshot(units, charges, payments, expenses)
                )
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("خلاصه ساختمان",style=MaterialTheme.typography.titleLarge)
                    SummaryCard("واحدهای ثبت‌شده",units.size.toString()); SummaryCard("بازه‌های شارژ",charges.size.toString()); SummaryCard("پرداخت‌های ثبت‌شده",payments.size.toString()); SummaryCard("هزینه‌های ثبت‌شده",expenses.size.toString())
                    Text("اطلاعات به‌صورت محلی در پایگاه داده Room/SQLite نگهداری می‌شود.",style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable private fun YearMonthFields(label1:String,value1:String,on1:(String)->Unit,label2:String,value2:String,on2:(String)->Unit) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value1,on1,label={Text(label1)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.weight(1f))
        OutlinedTextField(value2,on2,label={Text(label2)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.weight(1f))
    }
}
@Composable private fun MoneyField(value:String,onValue:(String)->Unit,label:String) = OutlinedTextField(value=value,onValueChange={onValue(it.filter(Char::isDigit).take(15))},label={Text(label)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.fillMaxWidth())
@Composable private fun SummaryCard(label:String,value:String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(label,style=MaterialTheme.typography.bodyMedium); Text(value,style=MaterialTheme.typography.titleLarge) } } }


private data class AssistantMessage(val text: String, val fromUser: Boolean)

@Composable
private fun BuildingAssistantScreen(snapshot: BuildingAssistant.Snapshot) {
    var question by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(AssistantMessage(
        "سلام! من دستیار آفلاین مدیرخانه هستم. درباره واحدها، ساکنین، شارژ، بدهی‌ها، پرداخت‌ها و هزینه‌ها سؤال کنید. برای دیدن نمونه سؤال‌ها بنویسید «راهنما»."
    , false)) }
    val suggestions = listOf("خلاصه وضعیت ساختمان", "چند واحد ثبت شده؟", "جمع پرداخت‌ها چقدر است؟", "هزینه‌های ساختمان چقدر شده؟", "راهنما")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("دستیار ساختمان · آفلاین", style = MaterialTheme.typography.titleMedium)
        Text("پاسخ‌ها بر اساس اطلاعات ذخیره‌شده روی همین گوشی است. این نسخه برای ثبت یا حذف اطلاعات، شما را به فرم مربوط هدایت می‌کند.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            suggestions.forEach { q -> AssistSuggestion(q) { question = q } }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { message ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (message.fromUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(message.text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(value = question, onValueChange = { question = it }, modifier = Modifier.weight(1f), placeholder = { Text("سؤالتان را بنویسید…") }, maxLines = 3)
            Button(onClick = {
                val q = question.trim()
                if (q.isNotEmpty()) {
                    messages.add(AssistantMessage(q, true))
                    messages.add(AssistantMessage(BuildingAssistant.answer(q, snapshot), false))
                    question = ""
                }
            }) { Text("ارسال") }
        }
    }
}

@Composable
private fun AssistSuggestion(text: String, onClick: () -> Unit) {
    AssistSuggestionChip(text, onClick)
}

@Composable
private fun AssistSuggestionChip(text: String, onClick: () -> Unit) {
    SuggestionChip(onClick = onClick, label = { Text(text) })
}
