package com.example.umd_gluten_free


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.umd_gluten_free.composables.ProgressBar
import com.example.umd_gluten_free.composables.MealCard
import com.example.umd_gluten_free.data.DataOrException
import com.example.umd_gluten_free.data.Meal
import com.example.umd_gluten_free.extra.MealsViewModel
import com.example.umd_gluten_free.ui.theme.UMDGlutenFreeTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.*


class MainActivity : ComponentActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var mealArrayList: ArrayList<Meal>
    private lateinit var myAdapter: MyAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private val viewModel: MealsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        db = Firebase.firestore
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            val dataOrException = viewModel.data.value
            UMDGlutenFreeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    AppNavHost(context = LocalContext.current, listenerOwner = this, auth = auth, db = db, viewModel = viewModel, dataOrException = dataOrException)

                }
            }
        }
    }

}

@Composable
fun AppNavHost(
    auth: FirebaseAuth,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "mapScreen",
    context: Context,
    listenerOwner: MainActivity,
    db: FirebaseFirestore,
    dataOrException: DataOrException<List<Meal>, Exception>,
    viewModel: MealsViewModel
) {
    NavHost(modifier=modifier, navController=navController, startDestination=startDestination) {
        composable("accountManagement") {
            if (auth.currentUser != null) {
                // If the user is currently logged in
                LogoutScreen(auth = auth, onNavigateToMap = {navController.navigate("mapScreen")})
            } else {
                LoginScreen(
                    onNavigateToForgotPass = {navController.navigate("forgotPassword")},
                    onNavigateToSignup = {navController.navigate("signupScreen")},
                    auth = auth,
                    context = context,
                    { navController.navigate("mapScreen") },
                )
            }
        }
        composable("listScreen") { ListScreen(db = db, context = Dispatchers.Default, viewModel = viewModel, dataOrException = dataOrException) }
        composable("submitNewFood") {
            if(auth.currentUser != null) {
                SubmitScreen(
                    auth = auth,
                    context = context,
                    //onNavigateToLogin = { navController.navigate("loginScreen") },
                    db = db,
                    coroutineContext = Dispatchers.Default
                )
            } else {
                Toast.makeText(context, "You cannot submit unless you are logged in.", Toast.LENGTH_LONG).show()
                LoginScreen(
                    onNavigateToForgotPass = {navController.navigate("forgotPassword")},
                    onNavigateToSignup = {navController.navigate("signupScreen")},
                    auth = auth,
                    context = context,
                    onNavigateToMap = {
                        navController.navigate("mapScreen")
                    },

                )
            }
        }
        composable("forgotPassword") {ForgotPasswordScreen(auth = auth, context = context)}
        composable("signupScreen") {SignupScreen(
            auth = auth,
            context = context,
            listenerOwner = listenerOwner,
            onNavigateToMap = {navController.navigate("mapScreen")})}
        composable("loginScreen") {
            LoginScreen(
                onNavigateToForgotPass = {navController.navigate("forgotPassword")},
                onNavigateToSignup = {navController.navigate("signupScreen")},
                auth = auth,
                context = context,
                onNavigateToMap = {
                    navController.navigate("mapScreen")

                                  },
            )
        }
        composable("mapScreen") {
            MapScreen(
                onNavigateToAcctManagement = {
                    navController.navigate("accountManagement")
                },
                onNavigateToList = {
                    navController.navigate("listScreen")
                },
                onNavigateToSubmit = {
                    navController.navigate("submitNewFood")
                },
                onNavigateToMap = {
                    navController.navigate("mapScreen")
                },
                auth = auth,
                db = db
            )
        }


    }
}
// Top level screens
@Composable

fun SubmitScreen(
    auth: FirebaseAuth,
    context: Context,
    db: FirebaseFirestore,
    coroutineContext: CoroutineDispatcher
) {
    suspend fun locationNameIsInDatabase(placeName: String): Boolean {
        var found = false
        return withContext(CoroutineScope(coroutineContext).coroutineContext) {
            db.collection("Locations").get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        for (document in it.result) {
                            if (document.data["locationName"].toString().trim()
                                    .lowercase() == placeName.trim().lowercase()
                            ) {
                                found = true
                            }
                        }
                    }
                }
            found
        }
    }
    suspend fun translateToGeoPoint(placeName: String): GeoPoint {
        // if the name is already in our database, return that geopoint. Otherwise, convert via geocoder
        if(locationNameIsInDatabase(placeName)){
            return CoroutineScope(coroutineContext).async {
                var point = GeoPoint(0.0, 0.0)
                db.collection("Locations").get()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            for (document in it.result) {
                                if (document.data["locationName"].toString().trim()
                                        .lowercase() == placeName.trim().lowercase()
                                ) {
                                    point = document.data["locationPoint"] as GeoPoint
                                }
                            }
                        }
                    }
                point
            }.await()
        }
        else {
            // actually figure out how the hell to do that lmao
            return GeoPoint(0.0, 0.0)
        }
    }

    suspend fun isInCollegePark(locationPoint: GeoPoint): Boolean {
        fun haversine(point1: GeoPoint, point2: GeoPoint): Double
        {
            var lat1 = point1.latitude
            var lat2 = point2.latitude
            val lon1 = point1.longitude
            val lon2 = point2.longitude

            // distance between latitudes and longitudes
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)

            // convert to radians
            lat1 = Math.toRadians(lat1)
            lat2 = Math.toRadians(lat2)

            // apply formulae
            val a: Double = sin(dLat / 2).pow(2) +
            sin(dLon / 2).pow(2.0) *
                    cos(lat1) *
                    cos(lat2)
            val rad = 6371.0
            val c: Double = 2 * asin(sqrt(a))
            return rad * c
            // returns distance between two geopoints in kilometers.
        }
        // if distance from UMD is less than ten miles (20km), true. else false
        return (haversine(locationPoint, translateToGeoPoint("University Of Maryland")) < 20.0)
    }

    suspend fun submitMeal(mealName:String, locationName: String, rating: Int) {
        val geoPoint = translateToGeoPoint(locationName)
        if (!isInCollegePark(geoPoint)) {
            Toast.makeText(context, "Location could not be parsed. Please check for typos and try again.", Toast.LENGTH_SHORT).show()
            return
        }
        // so if the location already exists in our database, fine! if not, we should add it.
        val meal = hashMapOf(
            "mealName" to mealName,
            "location" to locationName,
            "rating" to rating
        )
        db.collection("Meals")
            .add(meal)
            .addOnCompleteListener {
                if(it.isSuccessful) {

                }
                else {
                    Toast.makeText(context, "Error submitting: " + it.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        val centered = Modifier.align(Alignment.CenterHorizontally)
        val mealName = remember { mutableStateOf(TextFieldValue()) }
        val mealLocation = remember { mutableStateOf(TextFieldValue()) }
        val rating = remember { mutableStateOf(0f) }
        val isVegan = remember { mutableStateOf(true) }
        Spacer(modifier = Modifier.height(80.dp))
        TextField(
            label = { Text(text = "What did you eat?") },
            value = mealName.value,
            onValueChange = { mealName.value = it },
            modifier = centered
        )

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text("Where did you eat it?") },
            value = mealLocation.value,
            onValueChange = { mealLocation.value = it },
            modifier = centered
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(text="What would you rate it? (0-5)", modifier = Modifier.align(Alignment.CenterHorizontally))
        Slider(
            value = rating.value,
            onValueChange = {rating.value = it},
            valueRange = 0f..5f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = Color.Red,
                activeTrackColor = Color.Red
            ),
            modifier = Modifier.padding(60.dp, 10.dp)
        )
        Spacer(modifier = Modifier.height(30.dp))
        Button(
            onClick = {
                runBlocking {
                    submitMeal(
                        mealName.value.toString(),
                        mealLocation.value.toString(),
                        rating.value.toInt()
                    )
                }
            },
            modifier = centered,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
        ) {
            Text("Submit Meal", color = Color.White)
        }
    }

}

@Composable
fun ListScreen(db: FirebaseFirestore, context: CoroutineContext, viewModel: MealsViewModel, dataOrException: DataOrException<List<Meal>, Exception>) {
    val meals = dataOrException.data
    meals?.let {
        LazyColumn {
            items(
                items = meals
            ) { meal ->
                MealCard(meal = meal)
            }
        }
    }

    val e = dataOrException.e
    e?.let {
        Text(
            text = e.message!!,
            modifier = Modifier.padding(16.dp)
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProgressBar(
            isDisplayed = viewModel.loading.value
        )
    }
}
    /** I don't know if this is code for compose or not

    recyclerView = findViewById(R.id.recyclerView)
    recyclerView.LayoutManager = LinearLayoutManager(this)
    recyclerView.setHasFixedSize(true)

    mealArrayList = arrayListOf()

    myAdapter = myAdapter(mealArrayList)

    recyclerView.adapter = myAdapter

    EventChangeListener()

    suspend fun getLocations(): List<String> {
        return CoroutineScope(context).async {
            val locationData = db.collection("Locations")
            val list = mutableListOf<String>()

            locationData.get().addOnSuccessListener { documents ->
                for (document in documents) {
                    val restaurant = document.data["name:"].toString().trim()
                    val coordinates = document.data["locationPoint:"].toString().trim()

                }
            }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
            list
        }.await()

        //Placeholder(component = "List View")
    }
**/


@Composable
fun EventChangeListener(db: FirebaseFirestore, mealArrayList: ArrayList<Meal>) {
    db.collection("Meals").addSnapshotListener(object: EventListener<QuerySnapshot> {
        override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
            if (error != null) {
                Log.e("Firestore Error", error.message.toString())
                return
            }

            for (doc: DocumentChange in value?.documentChanges!!) {
                if (doc.type == DocumentChange.Type.ADDED) {
                    mealArrayList.add(doc.document.toObject(Meal::class.java))
                }
            }
        }
    })
}


@Composable
fun SignupScreen(auth: FirebaseAuth, context: Context, listenerOwner: ComponentActivity, onNavigateToMap: () -> Unit) {
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val email = remember { mutableStateOf(TextFieldValue()) }
        val password = remember { mutableStateOf(TextFieldValue()) }
        val vegan = remember { mutableStateOf(false) }
        Text(text = "Sign Up", style = TextStyle(fontSize = 40.sp))

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Email Address") },
            value = email.value,
            onValueChange = { email.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Password (6 characters or more)") },
            value = password.value,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = { password.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = "Vegan?")
        Switch(modifier = Modifier.align(Alignment.CenterHorizontally), checked = vegan.value, onCheckedChange = {vegan.value = it})

        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
            Button(
                onClick = {
                          auth.createUserWithEmailAndPassword(
                              email.value.text.trim(),
                              password.value.text.trim()
                          ).addOnCompleteListener {
                              if (it.isSuccessful) {
                                  Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                                  onNavigateToMap()
                              } else {
                                  Toast.makeText(context, it.exception?.message, Toast.LENGTH_LONG).show()
                              }
                          }
                },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
            ) {
                Text(text = "Sign Up", color = Color.White)
            }
        }
    }
}

@Composable

fun ForgotPasswordScreen(auth: FirebaseAuth, context: Context) {
    val email = remember { mutableStateOf(TextFieldValue()) }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Reset your password: ", modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(60.dp))
        TextField(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            value = email.value,
            onValueChange = {email.value = it},
            label = {Text("email address")})
        Spacer(modifier = Modifier.height(60.dp))
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {auth.sendPasswordResetEmail(email.value.text.trim())},
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
        ) {
            Text(text = "Send reset email", color = Color.White)
        }
    }
}

//sub-screens
@Composable
fun LoginScreen(
    onNavigateToForgotPass: () -> Unit,
    onNavigateToSignup: () -> Unit,
    auth: FirebaseAuth,
    context: Context,
    onNavigateToMap: () -> Unit,

) {
    Box(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onNavigateToSignup,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
            modifier = Modifier.align(Alignment.BottomCenter)) {
            Text("Sign Up")
        }
    }
    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val email = remember { mutableStateOf(TextFieldValue()) }
        val password = remember { mutableStateOf(TextFieldValue()) }

        Text(text = "Login Or Sign Up", style = TextStyle(fontSize = 40.sp))

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Username") },
            value = email.value,
            onValueChange = { email.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Password") },
            value = password.value,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = { password.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
            Button(
                onClick = {
                    auth.signInWithEmailAndPassword(
                        email.value.text.trim(),
                        password.value.text.trim()
                    ).addOnCompleteListener {
                        if(it.isSuccessful) {
                            //redirect to map, login-successful toast message
                            onNavigateToMap()
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_LONG).show()
                        }
                        else {
                            // toast failure message
                            Toast.makeText(context, "Error: " + it.exception?.message, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFe21833))
            ) {
                Text(text = "Login", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onNavigateToForgotPass,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)) {
            Text("Forgot Password?")
        }
    }
}

@Composable
fun LogoutScreen(onNavigateToMap: () -> Unit, auth: FirebaseAuth) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Would you like to logout?")
        Button(modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                auth.signOut()
                onNavigateToMap()
            }
        ) {
            Text("Yes")
        }
    }
}
// building blocks
@Composable
fun Placeholder(component: String) {
    Text(text = "$component goes here")
}

@Composable
fun TopBar(onMenuClicked: () -> Unit) {
    TopAppBar(
        title = {Text(text= "UMD Gluten Free", color=Color.White)},
        navigationIcon = {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",

                // When clicked trigger onClick
                // Callback to trigger drawer open
                modifier = Modifier.clickable(onClick = onMenuClicked),
                tint = Color.White
            )
            // A button to move map to user's location
        },
        backgroundColor = Color(0xFFe21833)
    )
}

@Composable
fun Drawer(
    onNavigateToAcctManagement: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToSubmit: () -> Unit,
    onNavigateToMap: () -> Unit,
    auth: FirebaseAuth
) {
    Column(
        Modifier.fillMaxSize()
    ) {
        //first we want a logo or something so we can put the buttons closer to the
        // user's fingers
        val alignToCenter = Modifier.align(Alignment.CenterHorizontally)
        Image(
            painter = painterResource(id = R.drawable.umd_gluten_free_logo),
            contentDescription = "",
            modifier = alignToCenter.size(150.dp)
        )
        TextButton(
            onClick = onNavigateToSubmit,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text("Submit New Meal", color=Color.Black)}
        TextButton(
            onClick = onNavigateToMap,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text("Map View", color=Color.Black)}
        TextButton(
            onClick = onNavigateToList,
            modifier = alignToCenter.fillMaxWidth()
        ) {Text("List View", color=Color.Black)}
        TextButton(
            onClick = onNavigateToAcctManagement,
            modifier = alignToCenter.fillMaxWidth()
            ) {Text( if (auth.currentUser != null) "Log out" else "Log in" , color=Color.Black)}
    }
}

@Composable
fun DrawerBody() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
    ) {
        GoogleMap(cameraPositionState = CameraPositionState(position = CameraPosition(LatLng(38.9779990, -76.9287295), 15f, 0f, 0f)))
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MapScreen(
    onNavigateToAcctManagement: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToSubmit: () -> Unit,
    onNavigateToMap: () -> Unit,
    auth: FirebaseAuth,
    db: FirebaseFirestore
) {
    // to set menu closed by default
    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val coroutineScope = rememberCoroutineScope()

    // Scaffold Composable
    Scaffold(

        // pass the scaffold state
        scaffoldState = scaffoldState,
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
        // pass the topbar we created
        topBar = {
            TopBar(
                // When menu is clicked open the
                // drawer in coroutine scope
                onMenuClicked = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.open()
                    }
                })
        },


        // Pass the body in
        // content parameter

        // THIS COULD BE A PROBLEM >:O
        content = {
            DrawerBody()
        },

        // pass the drawer
        drawerContent = {
            Drawer(
                onNavigateToAcctManagement = onNavigateToAcctManagement,
                onNavigateToList = onNavigateToList,
                onNavigateToSubmit = onNavigateToSubmit,
                onNavigateToMap = onNavigateToMap,
                auth =  auth
            )
        }
    )
}

