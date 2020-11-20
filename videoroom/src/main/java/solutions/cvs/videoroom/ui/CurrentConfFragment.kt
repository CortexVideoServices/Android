package solutions.cvs.videoroom.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import solutions.cvs.videoroom.ConferenceVM
import solutions.cvs.videoroom.R
import solutions.cvs.videoroom.UserSession
import solutions.cvs.videoroom.databinding.CurrentConferenceBinding


/**
 * Current session fragment
 */
class CurrentConferenceFragment : Fragment() {

    private val userSession: UserSession by activityViewModels()
    private val conferenceVM: ConferenceVM by activityViewModels()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return DataBindingUtil.inflate<CurrentConferenceBinding>(
            inflater,
            R.layout.fragment_current_conf,
            container,
            false
        ).also {
            it.lifecycleOwner = viewLifecycleOwner
            it.conferenceVM = conferenceVM
            it.userSession = userSession
        }.root
    }

}