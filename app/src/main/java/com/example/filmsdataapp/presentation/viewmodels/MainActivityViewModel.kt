package com.example.filmsdataapp.presentation.viewmodels

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.credentials.CredentialManager
import android.util.Log
import com.example.filmsdataapp.domain.model.ActivityData
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.mutableStateOf
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filmsdataapp.R

import com.example.filmsdataapp.domain.model.Actor
import com.example.filmsdataapp.domain.model.ActorInfo
import com.example.filmsdataapp.domain.model.FilterStatus
import com.example.filmsdataapp.domain.model.News
import com.example.filmsdataapp.domain.model.Review
import com.example.filmsdataapp.domain.model.SORTED_BY
import com.example.filmsdataapp.domain.model.Title
import com.example.filmsdataapp.domain.usecase.AddTitleToCompleteListUseCase
import com.example.filmsdataapp.domain.usecase.AddTitleToDroppedListUseCase
import com.example.filmsdataapp.domain.usecase.AddTitleToOnHoldListUseCase
import com.example.filmsdataapp.domain.usecase.AddTitleToPlannedListUseCase
import com.example.filmsdataapp.domain.usecase.AddTitleToWatchingListUseCase
import com.example.filmsdataapp.domain.usecase.ChangeUserAccountUseCase
import com.example.filmsdataapp.domain.usecase.CheckIfUserHasTitleInListsUseCase
import com.example.filmsdataapp.domain.usecase.CreateUserAccountUseCase
import com.example.filmsdataapp.domain.usecase.DeleteTitleFromAllListsUseCase
import com.example.filmsdataapp.domain.usecase.FetchUserHistoryUseCase

import com.example.filmsdataapp.domain.usecase.GetActorBioByIdUseCase
import com.example.filmsdataapp.domain.usecase.GetActorInfoByIdUseCase
import com.example.filmsdataapp.domain.usecase.GetActorsUseCase
import com.example.filmsdataapp.domain.usecase.GetComingSoonMoviesUseCase
import com.example.filmsdataapp.domain.usecase.GetCompletedTitlesUseCase
import com.example.filmsdataapp.domain.usecase.GetCurrentlyTrendingMoviesUseCase
import com.example.filmsdataapp.domain.usecase.GetDroppedTitlesUseCase
import com.example.filmsdataapp.domain.usecase.GetMonthlyCompletedStatsUseCase
import com.example.filmsdataapp.domain.usecase.GetMostPopularMoviesUseCase
import com.example.filmsdataapp.domain.usecase.GetMostPopularTVShowsUseCase
import com.example.filmsdataapp.domain.usecase.GetNewsUseCase
import com.example.filmsdataapp.domain.usecase.GetPlannedTitlesUseCase
import com.example.filmsdataapp.domain.usecase.GetReviewsByIdUseCase
import com.example.filmsdataapp.domain.usecase.GetTitleByIdUseCase
import com.example.filmsdataapp.domain.usecase.GetUserDescriptionUseCase
import com.example.filmsdataapp.domain.usecase.GetUserImageUseCase
import com.example.filmsdataapp.domain.usecase.GetUserNickNameUseCase
import com.example.filmsdataapp.domain.usecase.GetUserRatingFotTitleUseCase
import com.example.filmsdataapp.domain.usecase.GetWatchingTitlesUseCase
import com.example.filmsdataapp.domain.usecase.SearchTitleUseCase
import com.example.filmsdataapp.domain.usecase.SetUserRatingFotTitleUseCase
import com.example.filmsdataapp.domain.usecase.ValidateAuthenticationInputUseCase
import com.example.filmsdataapp.presentation.common.NavigationEvent
import com.example.filmsdataapp.presentation.utils.NetworkMonitor
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getActorInfoByIdUseCase: GetActorInfoByIdUseCase,
    private val getActorsUseCase: GetActorsUseCase,
    private val getComingSoonMoviesUseCase: GetComingSoonMoviesUseCase,
    private val getCurrentlyTrendingMoviesUseCase: GetCurrentlyTrendingMoviesUseCase,
    private val getMostPopularMoviesUseCase: GetMostPopularMoviesUseCase,
    private val getMostPopularTVShowsUseCase: GetMostPopularTVShowsUseCase,
    private val getNewsUseCase: GetNewsUseCase,
    private val getReviewsByIdUseCase: GetReviewsByIdUseCase,
    private val searchTitleUseCase: SearchTitleUseCase,
    private val validateAuthenticationInputUseCase : ValidateAuthenticationInputUseCase,
    private val createUserAccountUseCase : CreateUserAccountUseCase,
    private val setUserRatingFotTitleUseCase : SetUserRatingFotTitleUseCase,
    private val getUserRatingFotTitleUseCase : GetUserRatingFotTitleUseCase,
    private val changeUserAccountUseCase : ChangeUserAccountUseCase,
    private val getUserImageUseCase: GetUserImageUseCase,
    private val getMonthlyCompletedStatsUseCase: GetMonthlyCompletedStatsUseCase,
    private val checkIfUserHasTitleInListsUseCase : CheckIfUserHasTitleInListsUseCase,
    private val deleteTitleFromAllListsUseCase : DeleteTitleFromAllListsUseCase,
    private val getUserNickNameUseCase : GetUserNickNameUseCase,
    private val fetchUserHistoryUseCase : FetchUserHistoryUseCase,
    private val addTitleToWatchingListUseCase : AddTitleToWatchingListUseCase,
    private val addTitleToPlannedListUseCase : AddTitleToPlannedListUseCase,
    private val addTitleToCompleteListUseCase : AddTitleToCompleteListUseCase,
    private val addTitleToOnHoldListUseCase : AddTitleToOnHoldListUseCase,
    private val addTitleToDroppedListUseCase : AddTitleToDroppedListUseCase,
    private val getDroppedTitlesUseCase : GetDroppedTitlesUseCase,
    private val getCompletedTitlesUseCase : GetCompletedTitlesUseCase,
    private val getWatchingTitlesUseCase : GetWatchingTitlesUseCase,
    private val getPlannedTitlesUseCase : GetPlannedTitlesUseCase,
    private val getUserDescriptionUseCase : GetUserDescriptionUseCase,
    ) : ViewModel() {
    val filterStatus : FilterStatus = FilterStatus()
    private var networkMonitor = NetworkMonitor(context)
    val isConnected: StateFlow<Boolean> = networkMonitor.networkStatus
    var credentialManager : CredentialManager? = null
    lateinit var firebaseAuth : FirebaseAuth
    private val _navigation = MutableStateFlow<NavigationEvent>(NavigationEvent.None)
    val navigation: StateFlow<NavigationEvent> = _navigation
    var searchEnded = MutableLiveData<Boolean>()
    var showWarningInLogInScreen = MutableLiveData(false)
    var showWarningInPhoneNumberScreen = MutableLiveData<Boolean>(false)
    var warningInPhoneNumberScreen = MutableLiveData<String>("")

    var showSignInUsingGoogleOption = MutableLiveData<Boolean>()

    var userSuccessfullySignedIn =  MutableLiveData<Boolean>()

    var appGotUserPhoneNumber = MutableLiveData(false)

    var enteredPhoneNumber = MutableLiveData("")


    var recievedActorInfo = MutableLiveData<Boolean>(false)
    private var _mostPopularMovies = MutableLiveData<List<Title>>()
    private var _comingSoonMovies = MutableLiveData<List<Title>>()
    private var _mostPopularTVShows = MutableLiveData<List<Title>>()
    private val _searchedTitles = MutableLiveData<List<Title>>()
    private val _news = MutableLiveData<List<News>>()
    private val _actors = MutableLiveData<List<Actor>>()
    var searchedQuery = MutableLiveData<String>()

    var displayedActorInfo = MutableLiveData<ActorInfo>(null)
    private val _currentlyTrendingMovies = MutableLiveData<List<Title>>()
    var _inititalTitleToDisplay = MutableLiveData<List<Title>>()

    val _titlesToDisplay = MutableLiveData<List<Title>>()
    val _reviewsToDisplay = MutableLiveData<List<Review>>()

    val titlesToDisplay: LiveData<List<Title>> get() = _titlesToDisplay
    val searchedTitles: LiveData<List<Title>> get() = _searchedTitles
    val reviewsToDisplay: LiveData<List<Review>> get() = _reviewsToDisplay
    val mostPopularMovies: LiveData<List<Title>> get() = _mostPopularMovies
    val comingSoonMovies: LiveData<List<Title>> get() = _comingSoonMovies
    val mostPopularTVShows: LiveData<List<Title>> get() = _mostPopularTVShows

    private val _listOfTitlesToDisplay = mutableStateOf<List<Title>>(emptyList())
    val listOfTitlesToDisplay: State<List<Title>> = _listOfTitlesToDisplay

    val news : LiveData<List<News>> get() = _news
    val currentlyTrendingMovies: LiveData<List<Title>> get() = _currentlyTrendingMovies
    val actors: LiveData<List<Actor>> get() = _actors
    private var db : FirebaseFirestore = Firebase.firestore
    private var storedVerificationId: String? = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    var callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            if (e is FirebaseAuthInvalidCredentialsException) {
                showWarningInPhoneNumberScreen.value = true
                warningInPhoneNumberScreen.value = "Verification code is incorrect"
                // Invalid request
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
            } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
                // reCAPTCHA verification attempted with null Activity
            }

            showWarningInPhoneNumberScreen.value = true
            warningInPhoneNumberScreen.value = "Something went wrong, try again later"
        }
        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            Log.d("DEBUG", "onCodeSent:$verificationId")
            storedVerificationId = verificationId
            resendToken = token

        }
    }



    //NAVIGATION

    fun clearNavigation() {
        _navigation.value = NavigationEvent.None
    }

    fun onOnHoldListClicked(){
        _navigation.value = NavigationEvent.ToUserListOfTitles("onhold")
    }

    fun onDroppedListClicked(){
        _navigation.value = NavigationEvent.ToUserListOfTitles("dropped")
    }

    fun onCompletedListClicked(){
        _navigation.value = NavigationEvent.ToUserListOfTitles("completed")
    }

    fun onWatchingListClicked(){
        _navigation.value = NavigationEvent.ToUserListOfTitles("watching")
    }

    fun onPlannedListClicked(){
        _navigation.value = NavigationEvent.ToUserListOfTitles("planned")
    }

    fun getListOfTitlesByName(name: String) {
        when (name) {
            "onhold" -> getOnHoldTitles { _listOfTitlesToDisplay.value = it }
            "dropped" -> getDroppedTitles { _listOfTitlesToDisplay.value = it }
            "completed" -> getCompletedTitles { _listOfTitlesToDisplay.value = it }
            "watching" -> getWatchingTitles { _listOfTitlesToDisplay.value = it }
            "planned" -> getPlannedTitles { _listOfTitlesToDisplay.value = it }
        }
    }
    fun onNewsClicked(news: News) {
        _navigation.value = NavigationEvent.ToNews(news)
    }

    fun onTitleClicked(title: Title) {
        _navigation.value = NavigationEvent.ToTitle(title)
    }

    fun onCurrentlyTrendingTitlesClicked(){
        _navigation.value = NavigationEvent.ToCurrentlyTrendingTitles
    }

    fun onComingSoonTitlesClicked(){
        _navigation.value = NavigationEvent.ToComingSoonTitles
    }

    fun onTVShowsClicked(){
        _navigation.value = NavigationEvent.ToTVShow
    }

    fun onMoviesClicked(){
        _navigation.value = NavigationEvent.ToMovie
    }

    fun onUserHistoryClicked(){
        _navigation.value = NavigationEvent.ToUserHistory
    }

    fun onActorsClicked(){
        _navigation.value = NavigationEvent.ToActors
    }

    fun onMainClicked(){
        _navigation.value = NavigationEvent.ToMain
    }

    fun onProfileClicked(){
        _navigation.value = NavigationEvent.ToProfile
        _listOfTitlesToDisplay.value = emptyList()
    }

    fun onSearchTitleClicked(){
        _navigation.value = NavigationEvent.ToSearchTitle
    }

    fun onMenuClicked(){
        _navigation.value = NavigationEvent.OpenNav
    }

    fun onAuthClicked(){
        _navigation.value = NavigationEvent.ToAuth
    }
    fun onActorInfoClicked(){
        _navigation.value = NavigationEvent.ToActorInfo
    }

    fun onLogInClicked(){
        _navigation.value = NavigationEvent.ToLogIn
    }

    fun onSignInWithTelephoneNumberClicked(){
        _navigation.value = NavigationEvent.ToPhoneNumberSignIn
    }

    fun onCreateProfileClicked(){
        _navigation.value = NavigationEvent.ToCreateProfile
    }
    fun onSettingsClicked(){
        _navigation.value = NavigationEvent.ToUserSettings
    }
    //-------------------------------------------------------------------------------
    fun loadInitialData(){
        viewModelScope.launch {
            try {
                val newsDeferred = async { getNewsUseCase() }
                val popularMoviesDeferred = async { getMostPopularMoviesUseCase() }
                val comingSoonDeferred = async { getComingSoonMoviesUseCase() }
                val actorsDeferred = async { getActorsUseCase() }
                val tvShowsDeferred = async { getMostPopularTVShowsUseCase() }
                val trendingDeferred = async { getCurrentlyTrendingMoviesUseCase() }

                _news.value = newsDeferred.await()
                _mostPopularMovies.value = popularMoviesDeferred.await()
                _comingSoonMovies.value = comingSoonDeferred.await()
                _actors.value = actorsDeferred.await()
                _mostPopularTVShows.value = tvShowsDeferred.await()
                _currentlyTrendingMovies.value = trendingDeferred.await()
            } catch (e: Exception) {
                Log.d("DEBUG", "Error: ${e.message}")
            }
        }
    }
    fun startInternetObserve(){
        networkMonitor = NetworkMonitor(context)
    }
    fun checkIfUserIsSignedIn() : Boolean{
        return firebaseAuth.currentUser != null
    }
    private val _authErrorFlow = MutableSharedFlow<Pair<String, String>>()
    val authErrorFlow = _authErrorFlow.asSharedFlow()

    fun inputWasSuccessfullyValidated(email: String, password: String): Pair<Boolean, String?> {
        val pair = validateAuthenticationInputUseCase(email, password)

        if (!pair.first) {
            viewModelScope.launch {
                _authErrorFlow.emit(pair.second)
            }
            return false to pair.second.first
        }

        return true to null
    }

    fun setUserRatingForTitle(title: Title, rating: Float, where: String) {
        setUserRatingFotTitleUseCase(title, rating, where)
    }

    fun getUserRatingForTitle(titleId: String, where: String, onResult: (Int?) -> Unit) {
        getUserRatingFotTitleUseCase(titleId,where,onResult)
    }

    fun createUser(nickname: String, description: String, image: Uri?) {
        createUserAccountUseCase(nickname, description, image) { onMainClicked() }
    }

    fun changeUserAccount(nickname: String, description: String, image: Uri?) {
        changeUserAccountUseCase(nickname, description, image) { onProfileClicked() }
    }
    fun getUserImage(callback: (Uri) -> Unit) {
        getUserImageUseCase(callback)
    }

    fun getMonthlyCompletedStats(uid: String, onResult: (List<ActivityData>) -> Unit) {
        getMonthlyCompletedStatsUseCase(uid, onResult)
    }

    fun getUserWatchingTime(callback: (Int) -> Unit) {
        val listOfTitles = mutableListOf<Title>()
        var remainingCallbacks = 5

        fun onListLoaded(titles: List<Title>) {
            listOfTitles.addAll(titles)
            remainingCallbacks--
            if (remainingCallbacks == 0) {
                val totalHours = listOfTitles.sumOf {
                    it.runtimeMinutes?.let { mins -> Math.round(mins.toDouble() / 60.0).toInt() } ?: 0
                }
                callback(totalHours)
            }
        }

        getPlannedTitles { onListLoaded(it) }
        getWatchingTitles { onListLoaded(it) }
        getCompletedTitles { onListLoaded(it) }
        getOnHoldTitles { onListLoaded(it) }
        getDroppedTitles { onListLoaded(it) }
    }
    fun checkIfUserHasTitleInLists(titleId: String, callback: (String) -> Unit) {
        checkIfUserHasTitleInListsUseCase(titleId, callback)
    }

    suspend fun deleteTitleFromAllLists(id: String) {
        deleteTitleFromAllListsUseCase(id)
    }
    fun getUserNickname(callback: (String) -> Unit) {
        getUserNickNameUseCase(callback)
    }

    fun addTitleToWatchingList(title: Title) {
        viewModelScope.launch {
            deleteTitleFromAllListsUseCase(title.id!!)
            addTitleToWatchingListUseCase(title)
        }
    }


    fun fetchUserHistory(onResult: (List<String>) -> Unit, onError: (Exception) -> Unit) {
        fetchUserHistoryUseCase(onResult, onError)
    }
    fun addTitleToPlannedList(title: Title) {
        viewModelScope.launch {
            deleteTitleFromAllListsUseCase(title.id!!)
            addTitleToPlannedListUseCase(title)
        }
    }

    fun addTitleToCompletedList(title: Title) {
        viewModelScope.launch {
            deleteTitleFromAllListsUseCase(title.id!!)
            addTitleToCompleteListUseCase(title)
        }
    }

    fun addTitleToOnHoldList(title: Title) {
        viewModelScope.launch {
            deleteTitleFromAllListsUseCase(title.id!!)
            addTitleToOnHoldListUseCase(title)
        }
    }

    fun addTitleToDroppedList(title: Title) {
        viewModelScope.launch {
            deleteTitleFromAllListsUseCase(title.id!!)
            addTitleToDroppedListUseCase(title)
        }
    }

    fun getOnHoldTitles(callback: (List<Title>) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(uid)
            .collection("onhold")
            .get()
            .addOnSuccessListener { result ->
                val titles = result.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Title::class.java)
                    } catch (e: Exception) {
                        Log.e("WATCHING", "Failed to parse title from Firestore", e)
                        null
                    }
                }
                callback(titles)
            }
            .addOnFailureListener { e ->
                Log.e("WATCHING", "Failed to get watching titles", e)
                callback(emptyList())
            }
    }
    fun getDroppedTitles(callback: (List<Title>) -> Unit) {
        getDroppedTitlesUseCase(callback)
    }
    fun getCompletedTitles(callback: (List<Title>) -> Unit) {
        getCompletedTitlesUseCase(callback)
    }

    fun getWatchingTitles(callback: (List<Title>) -> Unit) {
        getWatchingTitlesUseCase(callback)
    }

    fun getPlannedTitles(callback: (List<Title>) -> Unit) {
        getPlannedTitlesUseCase(callback)
    }

    fun getUserDescription(callback: (String) -> Unit) {
        getUserDescriptionUseCase(callback)
    }

    private val userImageUri = mutableStateOf<Uri?>(null)

    fun loadUserImage() {
        firebaseAuth.currentUser?.reload()?.addOnCompleteListener {
            val photoUrl = firebaseAuth.currentUser?.photoUrl
            userImageUri.value = photoUrl ?: Uri.parse("android.resource://com.example.filmsdataapp/${R.drawable.user_icon}")
        }
    }

    fun startPhoneNumberVerification(activity: Activity) {

        appGotUserPhoneNumber.value = true
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(enteredPhoneNumber.value!!) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(activity) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(code: String) {
        if(code.isEmpty()){
            showWarningInPhoneNumberScreen.value = true
            warningInPhoneNumberScreen.value = "Please enter verification code"
            return
        }
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    fun verifyPhoneNumber(number : String) : Boolean{
        if(number.isEmpty()){
            showWarningInPhoneNumberScreen.value = true
            warningInPhoneNumberScreen.value = "Please enter your phone number"
            return false
        }
        return true


    }
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser == true
                    if (isNewUser) {
                        onCreateProfileClicked()
                    } else {

                        onMainClicked()
                    }


                    userSuccessfullySignedIn.value = true

                } else {
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                }
            }
    }

    fun searchTitle(query:String){
        viewModelScope.launch {
            val df = async { searchTitleUseCase(query) }
            _searchedTitles.value = df.await()

        }.invokeOnCompletion {
            searchEnded.value = true
            Log.d("DEBUG", "SEARCHED TITLES " + searchedTitles.value!!.size.toString())
        }

    }


    fun signOut() {
        // Firebase sign out
        firebaseAuth.signOut()


        // When a user signs out, clear the current user credential state from all credential providers.
        viewModelScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager?.clearCredentialState(clearRequest)

            } catch (e: ClearCredentialException) {
                Log.e("DEBUG", "Couldn't clear user credentials: ${e.localizedMessage}")
            }
        }


    }


    fun stopSearching(){
        searchEnded.value = false
    }

    fun getTitleReviews(id:String){
        viewModelScope.launch {
            val df = async { getReviewsByIdUseCase(id) }
            val list = df.await()
            _reviewsToDisplay.value = list
        }
    }

    fun getActorInfo(id:String){
        try{
            viewModelScope.launch {
                val df = async { getActorInfoByIdUseCase(id) }
                displayedActorInfo.value = df.await()
            }.invokeOnCompletion {
                recievedActorInfo.value = true
            }
        }catch (_: Exception){

        }
    }

    fun applyFilter(){
        _titlesToDisplay.value = applyFiltersLogic(_inititalTitleToDisplay.value!!, filterStatus)
    }

    private fun applyFiltersLogic(movies: List<Title>, filter: FilterStatus): List<Title> {
        val afterGenre = movies.filter { movie ->
            val selectedGenres = filter.genre
                ?.mapNotNull { filter.genres[it]?.lowercase() }
                ?.takeIf { it.isNotEmpty() }

            if (selectedGenres != null) {
                val movieGenres = movie.genres?.map { it.lowercase() } ?: emptyList()
                movieGenres.any { it in selectedGenres }
            } else {
                true //
            }
        }

        val afterRating = afterGenre.filter { movie ->
            filter.averageRationFrom?.let { from ->
                movie.averageRating?.toInt()?.let { it >= from } ?: false
            } ?: true
        }

        val afterDate = afterRating.filter { movie ->
            val year = movie.startYear
            val fromOk = filter.dateOfReleaseFrom?.let { year != null && year >= it } ?: true
            val toOk = filter.dateOfReleaseTo?.let { year != null && year <= it } ?: true
            fromOk && toOk
        }

        val result = afterDate.sortedWith { a, b ->
            when (filter.sortedBy) {
                SORTED_BY.POPULARITY -> (b.numVotes ?: 0).compareTo(a.numVotes ?: 0)
                SORTED_BY.RATING -> (b.averageRating ?: 0f).compareTo(a.averageRating ?: 0f)
                SORTED_BY.ALPHABET -> (a.primaryTitle ?: "").compareTo(b.primaryTitle ?: "")
                SORTED_BY.RELEASE_DATE -> (b.startYear ?: 0).compareTo(a.startYear ?: 0)
                SORTED_BY.RANDOM -> listOf(-1, 1).random()
                null -> 0
            }
        }
        return result
    }

    fun handleSignIn(credential: Credential, componentActivity: ComponentActivity) {
        try{
            // Check if credential is of type Google ID
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                // Create Google ID Token
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                // Sign in to Firebase with using the token
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken, componentActivity)
            } else {
                Log.w("DEBUG", "Credential is not of type Google ID!")
            }
        }catch (e : Exception){
            Log.w("DEBUG", e)
        }

    }

    fun createUserWithEmailAndPassword(email : String, password: String){
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    onCreateProfileClicked()
                }else{
                }
            }
    }

    fun signInUserWithEmailAndPassword(email:String, password: String){
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    onMainClicked()
                } else {
                    Log.d("DEBUG", "signInWithEmail:failure", task.exception)
                }
            }.addOnFailureListener {

            }
    }


    private fun firebaseAuthWithGoogle(idToken: String, componentActivity: ComponentActivity) {
        try{
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(componentActivity) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("DEBUG", "signInWithCredential:success")
                        val isNewUser = task.result?.additionalUserInfo?.isNewUser == true
                        if (isNewUser) {
                            onCreateProfileClicked()
                        } else {
                            onMainClicked()
                        }

                        userSuccessfullySignedIn.value = true
                    } else {
                        // If sign in fails, display a message to the user
                        Log.w("DEBUG", "signInWithCredential:failure", task.exception)
                    }
                }.addOnFailureListener {
                    if (it is GetCredentialCancellationException) {

                        Log.i("Auth", "User cancelled credential selection")
                    } else {

                        Log.e("Auth", "Credential error", it)
                    }
                }
        } catch (e : Exception){
            Log.w("DEBUG", "signInWithCredential:failure", e)
        }

    }

}
