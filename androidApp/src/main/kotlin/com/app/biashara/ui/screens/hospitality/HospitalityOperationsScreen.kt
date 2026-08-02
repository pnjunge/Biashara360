package com.app.biashara.ui.screens.hospitality

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable private data class Ingredient(val id:String,val name:String,val unit:String,val quantity:Double,val reorderLevel:Double,val unitCost:Double,val isLowStock:Boolean)
@Serializable private data class Reservation(val id:String,val customerName:String,val guestCount:Int,val reservedAt:String,val status:String)
@Serializable private data class Shift(val id:String,val openedAt:String,val status:String,val openingFloat:Double,val variance:Double?=null)
@Serializable private data class Operations(val reservations:List<Reservation> = emptyList(),val ingredients:List<Ingredient> = emptyList(),val shifts:List<Shift> = emptyList())

@Composable fun HospitalityOperationsScreen(client:HttpClient=koinInject()){
 val scope=rememberCoroutineScope();val uriHandler=LocalUriHandler.current;var data by remember{mutableStateOf<Operations?>(null)};var error by remember{mutableStateOf<String?>(null)};var loading by remember{mutableStateOf(false)}
 fun load(){scope.launch{loading=true;runCatching{client.get("$BASE_URL/hospitality/operations").body<ApiResponse<Operations>>()}.onSuccess{if(it.success)data=it.data else error=it.message}.onFailure{error=it.message};loading=false}}
 LaunchedEffect(Unit){load()};LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("Hospitality Operations",fontSize=24.sp,fontWeight=FontWeight.Bold);Text("Reservations, shifts and ingredient controls",fontSize=12.sp,color=Color.Gray)};Button(onClick=::load){Text("Refresh")}}}
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={uriHandler.openUri("https://biashara360.co.ke/hospitality")},modifier=Modifier.weight(1f)){Text("Restaurant POS")};OutlinedButton(onClick={uriHandler.openUri("https://biashara360.co.ke/open-tabs")},modifier=Modifier.weight(1f)){Text("Open tabs")}}}
  item{Button(onClick={uriHandler.openUri("https://biashara360.co.ke/kitchen-display")},modifier=Modifier.fillMaxWidth()){Text("Open Kitchen & Bar Display")}}
  error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
  if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
  data?.let{ops->item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Summary("Bookings",ops.reservations.count{it.status=="BOOKED"},Modifier.weight(1f));Summary("Low stock",ops.ingredients.count{it.isLowStock},Modifier.weight(1f));Summary("Shift",if(ops.shifts.any{it.status=="OPEN"})1 else 0,Modifier.weight(1f))}};item{Text("Upcoming reservations",fontWeight=FontWeight.Bold)};items(ops.reservations.filter{it.status=="BOOKED"}){Card{Column(Modifier.fillMaxWidth().padding(14.dp)){Text(it.customerName,fontWeight=FontWeight.Bold);Text("${it.guestCount} guests · ${it.reservedAt}",fontSize=12.sp)}}};item{Text("Ingredient stock",fontWeight=FontWeight.Bold)};items(ops.ingredients){Card(colors=CardDefaults.cardColors(if(it.isLowStock)Color(0xFFFFF7ED) else Color.White)){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(it.name,fontWeight=FontWeight.Bold);Text("Reorder at ${it.reorderLevel} ${it.unit}",fontSize=11.sp,color=Color.Gray)};Text("${it.quantity} ${it.unit}",fontWeight=FontWeight.Bold)}}}}
 }}
@Composable private fun Summary(label:String,value:Int,modifier:Modifier){Card(modifier){Column(Modifier.padding(12.dp)){Text(value.toString(),fontSize=22.sp,fontWeight=FontWeight.Bold);Text(label,fontSize=11.sp,color=Color.Gray)}}}
