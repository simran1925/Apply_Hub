package com.community.api.services;

import com.community.api.component.Constant;
import com.community.api.entity.CustomJobGroup;
import com.community.api.entity.Qualification;
import com.community.api.services.exception.ExceptionHandlingImplement;
import com.community.api.utils.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import static com.community.api.component.Constant.FIND_ALL_QUALIFICATIONS_QUERY;

@Service
public class QualificationService {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ExceptionHandlingImplement exceptionHandlingService;
    @Autowired
    private QualificationService qualificationService;
    @Autowired
    private ResponseService responseService;
    public List<Qualification> getAllQualifications() {
        TypedQuery<Qualification> query = entityManager.createQuery(Constant.FIND_ALL_QUALIFICATIONS_QUERY, Qualification.class);
        List<Qualification> qualifications = query.getResultList();
        List<Qualification> filteredQualifications = qualifications.stream()
                .filter(q -> !q.getQualification_name().equalsIgnoreCase("BACHELORS/GRADUATION") &&
                        !q.getQualification_name().equalsIgnoreCase("MASTERS/POST_GRADUATION"))
                .collect(Collectors.toList());
        return filteredQualifications;
    }
//    @todo:- Need to work on add qualification function so that entries should be inserted in document table also make sure to add one exam text in dscription
    @Transactional
    public Qualification addQualification(@RequestBody Qualification qualification) throws Exception {
        Qualification qualificationToBeSaved =new Qualification();
        int id = findCount() + 1;
        if (qualification.getQualification_name() == null || qualification.getQualification_name().trim().isEmpty()) {
            throw new IllegalArgumentException("Qualification name cannot be empty or consist only of whitespace");
        }
        if (qualification.getQualification_description() == null || qualification.getQualification_description().trim().isEmpty()) {
            throw new IllegalArgumentException("Qualification description cannot be empty or consist only of whitespace");
        }
        if (!qualification.getQualification_name().matches("^[a-zA-Z ]+$")) {
            throw new IllegalArgumentException("Qualification name cannot contain numeric values or special characters");
        }
        if (!(qualification.getQualification_description() instanceof String)) {
            throw new IllegalArgumentException("Qualification description must be a string");
        }
        String description = qualification.getQualification_description();
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Qualification description cannot be empty");
        }


        List<Qualification> qualifications = qualificationService.getAllQualifications();
        for (Qualification existingQualification : qualifications) {
            if (existingQualification.getQualification_name().equalsIgnoreCase(qualification.getQualification_name())) {
                throw new IllegalArgumentException("Qualification with the same name already exists");
            }
        }
        qualificationToBeSaved.setQualification_id(id);
        qualificationToBeSaved.setQualification_name(qualification.getQualification_name());
        qualificationToBeSaved.setQualification_description(qualification.getQualification_description());
        entityManager.persist(qualificationToBeSaved);
        return qualificationToBeSaved;
    }

    //need to be change here
    public int findCount() throws Exception {
        try {
        String queryString = Constant.GET_QUALIFICATIONS_COUNT;
        TypedQuery<Integer> query = entityManager.createQuery(queryString, Integer.class);
        return query.getSingleResult();
        } catch (NoResultException e) {
            exceptionHandlingService.handleException(e);
            throw new NoResultException("No any qualification is found");
        }
        catch (Exception exception) {
            exceptionHandlingService.handleException(exception);
            throw new Exception("SOMETHING WENT WRONG: "+ exception.getMessage());
        }
    }

    public Qualification getQualificationByQualificationId(Integer qualificationId) throws Exception {
        try {

            Query query = entityManager.createQuery(Constant.GET_QUALIFICATION_BY_ID, Qualification.class);
            query.setParameter("qualificationId", qualificationId);
            List<Qualification> qualification = query.getResultList();

            if (!qualification.isEmpty()) {
                return qualification.get(0);
            } else {
                return null;
            }

        } catch (Exception exception) {
            exceptionHandlingService.handleException(exception);
            throw new Exception("SOMETHING WENT WRONG: "+ exception.getMessage());
        }
    }
}
