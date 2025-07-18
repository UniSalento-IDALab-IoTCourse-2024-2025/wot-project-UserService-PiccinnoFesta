package it.unisalento.pas2425.userserviceproject.dto;

import java.util.List;

public class PatientListDTO {

    private List<PatientDTO> patientList;

    public List<PatientDTO> getPatientList() {
        return patientList;
    }

    public void setPatientList(List<PatientDTO> patientList) {
        this.patientList = patientList;
    }
}