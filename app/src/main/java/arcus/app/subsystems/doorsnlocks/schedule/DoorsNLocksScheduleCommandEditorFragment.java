/*
 *  Copyright 2019 Arcus Project.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package arcus.app.subsystems.doorsnlocks.schedule;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import arcus.cornea.utils.DayOfWeek;
import arcus.cornea.utils.TimeOfDay;
import arcus.app.R;
import arcus.app.common.backstack.BackstackManager;
import arcus.app.common.error.ErrorManager;
import arcus.app.common.popups.ButtonListPopup;
import arcus.app.common.schedule.AbstractScheduleCommandEditorFragment;
import arcus.app.common.schedule.controller.ScheduleCommandEditController;
import arcus.app.common.schedule.model.AbstractScheduleCommandModel;
import arcus.app.common.schedule.model.ScheduleCommandModel;
import arcus.app.common.utils.StringUtils;
import arcus.app.device.settings.core.Setting;
import arcus.app.device.settings.core.SettingsList;
import arcus.app.device.settings.style.OnClickActionSetting;
import arcus.app.subsystems.doorsnlocks.schedule.model.GarageDoorCommand;
import arcus.app.subsystems.doorsnlocks.schedule.model.PetDoorCommand;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


public class DoorsNLocksScheduleCommandEditorFragment extends AbstractScheduleCommandEditorFragment implements ScheduleCommandEditController.Callbacks {

    private final static String DEVICE_NAME = "DEVICE_NAME";
    private final static String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final static String TIME_OF_DAY_COMMAND_ID = "TIME_OF_DAY_COMMAND_ID";
    private final static String CURRENT_DAY_OF_WEEK = "CURRENT_DAY_OF_WEEK";
    private static final String IS_PET_DOOR = "IS_PET_DOOR";
    private Boolean isPetDoor = false;

    private AbstractScheduleCommandModel petAndGarageCommandModel;

    public static DoorsNLocksScheduleCommandEditorFragment newEditEventInstance (String deviceAddress, String deviceName, String timeOfDayCommandId, DayOfWeek currentDayOfWeek, Boolean isPetDoor) {
        DoorsNLocksScheduleCommandEditorFragment instance = new DoorsNLocksScheduleCommandEditorFragment();
        Bundle arguments = new Bundle();
        arguments.putString(DEVICE_ADDRESS, deviceAddress);
        arguments.putString(DEVICE_NAME, deviceName);
        arguments.putString(TIME_OF_DAY_COMMAND_ID, timeOfDayCommandId);
        arguments.putSerializable(CURRENT_DAY_OF_WEEK, currentDayOfWeek);
        arguments.putBoolean(IS_PET_DOOR, isPetDoor);
        instance.setArguments(arguments);

        return instance;
    }

    public static DoorsNLocksScheduleCommandEditorFragment newAddEventInstance (String deviceAddress, String deviceName, DayOfWeek currentDayOfWeek, Boolean isPetDoor) {
        DoorsNLocksScheduleCommandEditorFragment instance = new DoorsNLocksScheduleCommandEditorFragment();
        Bundle arguments = new Bundle();
        arguments.putString(DEVICE_ADDRESS, deviceAddress);
        arguments.putString(DEVICE_NAME, deviceName);
        arguments.putSerializable(CURRENT_DAY_OF_WEEK, currentDayOfWeek);
        instance.setArguments(arguments);
        arguments.putBoolean(IS_PET_DOOR, isPetDoor);
        return instance;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        isPetDoor = getArguments().getBoolean(IS_PET_DOOR);
        if(isPetDoor){
            petAndGarageCommandModel= new PetDoorCommand();

        }
        else{
            petAndGarageCommandModel=new GarageDoorCommand();
        }

        setDeleteButtonVisibility(isEditMode() ? View.VISIBLE : View.GONE);

        return view;
    }


    @Override
    public void onResume () {
        super.onResume();

        ScheduleCommandEditController.getInstance().setListener(this);
        if (isEditMode()) {
            ScheduleCommandEditController.getInstance().loadCommand(getScheduledEntityAddress(), getTimeOfDayCommandId(), petAndGarageCommandModel);
        }
    }

    @Override
    public String getTitle () {
        return getDeviceName();
    }

    @Override
    public DayOfWeek getCurrentDayOfWeek() {
        return (DayOfWeek) getArguments().getSerializable(CURRENT_DAY_OF_WEEK);
    }

    @Override
    public List<DayOfWeek> getScheduledDaysOfWeek() {
        return new ArrayList<>(petAndGarageCommandModel.getDays());
    }

    @Override
    public TimeOfDay getScheduledTimeOfDay() {
        return petAndGarageCommandModel.getTime();
    }

    @Override
    public boolean isEditMode() {
        return getTimeOfDayCommandId() != null;
    }

    @Override
    public SettingsList getEditableCommandAttributes() {
        SettingsList settings = new SettingsList();

        // Always show the "STATE" option
        settings.add(buildStateSetting());

        // ... optionally show the "REPEAT ON" option when a repeat schedule is defined
        if (petAndGarageCommandModel.getDays().size() > 1) {
            settings.add(buildRepeatSetting());
        }

        return settings;
    }

    @Override
    public void onDeleteEvent() {
        ScheduleCommandEditController.getInstance().deleteCommand(getScheduledEntityAddress(), petAndGarageCommandModel);
    }

    @Override
    public void onRepeatChanged(Set repeatDays) {
        petAndGarageCommandModel.setDays(repeatDays);
        setRepeatRegionVisibility(repeatDays.size() > 1 ? View.GONE : View.VISIBLE);

        rebind();
    }

    @Override
    public void onSaveEvent(EnumSet selectedDays, TimeOfDay timeOfDay) {
        petAndGarageCommandModel.setDays(selectedDays);
        petAndGarageCommandModel.setTime(timeOfDay);

        if (isEditMode()) {
            ScheduleCommandEditController.getInstance().updateCommand(getScheduledEntityAddress(), petAndGarageCommandModel);
        } else {
            ScheduleCommandEditController.getInstance().addCommand(getScheduledEntityAddress(), petAndGarageCommandModel);
        }
    }

    @Override
    public void onSchedulerError(Throwable cause) {
        hideProgressBar();
        ErrorManager.in(getActivity()).showGenericBecauseOf(cause);
    }

    @Override
    public void onTimeOfDayCommandLoaded(ScheduleCommandModel scheduleCommandModel) {
        hideProgressBar();

        if(isPetDoor){
            this.petAndGarageCommandModel = (PetDoorCommand) scheduleCommandModel;
        }else{
            this.petAndGarageCommandModel = (GarageDoorCommand) scheduleCommandModel;
        }


        setRepeatRegionVisibility(petAndGarageCommandModel.getDays().size() > 1 ? View.GONE : View.VISIBLE);
        setSelectedDays(petAndGarageCommandModel.getDaysAsEnumSet());

        // Redraw the screen with the updated command values
        rebind();
    }

    @Override
    public void onSuccess() {
        hideProgressBar();
        goBack();
    }

    private Setting buildRepeatSetting () {
        String repeatAbstract = StringUtils.getScheduleAbstract(getActivity(), petAndGarageCommandModel.getDays());
        OnClickActionSetting repeatSetting = new OnClickActionSetting(getString(R.string.doors_and_locks_repeat_on), null, repeatAbstract);
        repeatSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRepeatPicker();
            }
        });

        return repeatSetting;
    }

    private Setting buildStateSetting () {
        String stateAbstract;
        OnClickActionSetting stateSetting;
        if(isPetDoor){
            stateAbstract = ((PetDoorCommand)petAndGarageCommandModel).getCommandAbstract();
            stateSetting = new OnClickActionSetting(getString(R.string.petdoor_mode), null, stateAbstract);

        }else{
           stateAbstract = ((GarageDoorCommand)petAndGarageCommandModel).isGarageDoorStateOpen() ? getString(R.string.doors_and_locks_state_open) : getString(R.string.doors_and_locks_state_close);
            stateSetting = new OnClickActionSetting(getString(R.string.doors_and_locks_state), null, stateAbstract);

        }
        stateSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptUserForStateSelection();
            }
        });

        return stateSetting;
    }

    private void promptUserForStateSelection () {
        HashMap<String,String> editChoices = new HashMap<>();
        int title;
        if(isPetDoor){
            editChoices.put(getString(R.string.petdoor_mode_auto), getString(R.string.petdoor_mode_auto));
            editChoices.put(getString(R.string.petdoor_mode_locked), getString(R.string.petdoor_mode_locked));
            editChoices.put(getString(R.string.petdoor_mode_unlocked), getString(R.string.petdoor_mode_unlocked));
            title = R.string.petdoor_picker_title;
        }else{
            editChoices.put(getString(R.string.doors_and_locks_state_open), getString(R.string.doors_and_locks_state_open));
            editChoices.put(getString(R.string.doors_and_locks_state_close), getString(R.string.doors_and_locks_state_close));
            title = R.string.doors_and_locks_state_title;
        }



        ButtonListPopup editWhichDayPopup = ButtonListPopup.newInstance(
                editChoices,
                title, -1);

        editWhichDayPopup.setCallback(new ButtonListPopup.Callback() {
            @Override
            public void buttonSelected(String buttonKeyValue) {
                BackstackManager.getInstance().navigateBack();
                if(isPetDoor){
                    ((PetDoorCommand)petAndGarageCommandModel).setLockState(buttonKeyValue);
                }else {
                    ((GarageDoorCommand)petAndGarageCommandModel).setGarageDoorStateOpen(getString(R.string.doors_and_locks_state_open).equals(buttonKeyValue));
                }
                rebind();
            }
        });

        BackstackManager.getInstance().navigateToFloatingFragment(editWhichDayPopup, editWhichDayPopup.getClass().getSimpleName(), true);
    }

    private String getDeviceName() {
        return getArguments().getString(DEVICE_NAME);
    }

    public String getScheduledEntityAddress() {
        return getArguments().getString(DEVICE_ADDRESS);
    }

    private String getTimeOfDayCommandId() {
        return getArguments().getString(TIME_OF_DAY_COMMAND_ID, null);
    }
}
