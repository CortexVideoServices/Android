package solutions.cvs.videoroom.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import solutions.cvs.videoroom.ConferenceVM
import solutions.cvs.videoroom.R


/**
 * Conference loader
 */
class LoaderFragment : Fragment() {
    private val conferenceVM: ConferenceVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_loader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        conferenceVM.currentConference()
        conferenceVM.conferenceReady.observe(viewLifecycleOwner) {ready ->
            if (ready) {
                if (conferenceVM.conferenceData.value != null) findNavController().navigate(R.id.action_2CurrentConference)
                else findNavController().navigate(R.id.action_2CreateConference)
            }
        }
    }
}