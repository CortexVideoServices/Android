package solutions.cvs.videoroom.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import solutions.cvs.videoroom.R
import solutions.cvs.videoroom.ConferenceVM
import solutions.cvs.videoroom.databinding.CreateConferenceBinding
import solutions.cvs.videoroom.UserSession


class CreateConfFragment : Fragment() {

    private val userSession: UserSession by activityViewModels()
    private val conferenceVM: ConferenceVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return DataBindingUtil.inflate<CreateConferenceBinding>(
            inflater,
            R.layout.fragment_conf_create,
            container,
            false
        ).also {
            it.lifecycleOwner = viewLifecycleOwner
            it.conferenceVM = conferenceVM
            it.userSession = userSession
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        conferenceVM.conferenceData.observe(viewLifecycleOwner) {conferenceData ->
            if (conferenceData !=null ) findNavController().navigate(R.id.action_Create2CurrentConference)
        }
    }

}