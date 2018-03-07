#include "Open62541/open62541.h"
#include <jni.h>
#include<stdio.h>
#include<string.h>
#include<pthread.h>
#include<stdlib.h>
#include<unistd.h>

/*
----------------------Additional functions to create node display.
*/

//Synchronization.
static pthread_mutex_t list_lock;


static void
handler_TheAnswerChanged(UA_UInt32 monId, UA_DataValue *value, void *context) {
    printf("The Answer has changed!\n");
}

static UA_StatusCode
nodeIter(UA_NodeId childId, UA_Boolean isInverse, UA_NodeId referenceTypeId, void *handle) {
    if(isInverse)
        return UA_STATUSCODE_GOOD;
    UA_NodeId *parent = (UA_NodeId *)handle;
    printf("%d, %d --- %d ---> NodeId %d, %d\n",
           parent->namespaceIndex, parent->identifier.numeric,
           referenceTypeId.identifier.numeric, childId.namespaceIndex,
           childId.identifier.numeric);
    return UA_STATUSCODE_GOOD;
}

static UA_Variant * parseType(const char* typeName)
{
    /* Write node attribute (using the highlevel API) */
    UA_Variant *myVariant = UA_Variant_new();

    /* Choose type of variable */
    if(!strcmp(typeName, "bool"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_BOOLEAN]);
        return myVariant;
    }

    if(!strcmp(typeName,"signed char"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_SBYTE]);
        return myVariant;
    }

    if(!strcmp(typeName,"unsigned char"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_BYTE]);
        return myVariant;
    }

    if(!strcmp(typeName,"short"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_INT16]);
        return myVariant;
    }

    if(!strcmp(typeName, "unsigned short"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_UINT16]);
        return myVariant;
    }

    if(!strcmp(typeName,"int"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_INT32]);
        return myVariant;
    }

    if(!strcmp(typeName,"unsigned"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_UINT32]);
        return myVariant;
    }

    if(!strcmp(typeName,"long"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_INT64]);
        return myVariant;
    }

    if(!strcmp(typeName,"unsigned long"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_UINT64]);
        return myVariant;
    }

    if(!strcmp(typeName,"float"))
    {        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_FLOAT]);

        return myVariant;
    }

    if(!strcmp(typeName,"double"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_DOUBLE]);
        return myVariant;
    }

    if(!strcmp(typeName,"string"))
    {
        UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_STRING]);
        return myVariant;
    }
    return NULL;
}

//Struct for full info about OPC UA clients.
typedef struct
{
    UA_Client * client;
    char* endpoint;
    char* nodeName;
    int nodeID;
    UA_Variant* variable;
    UA_StatusCode status;
}UA_Full_Client;

//Struct for clients list.
typedef struct UA_Client_List_Node {
    UA_Full_Client *node;
    int number;
    struct UA_Client_List_Node * prev;
    struct UA_Client_List_Node * next;
}UA_Client_List_Node;

//Sentinel
typedef struct
{
    UA_Client_List_Node * head;
    UA_Client_List_Node * tail;
    unsigned int count;
} List_Sentinel;

static List_Sentinel sentinel;

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cInitClientList(JNIEnv *env,
                                                                              jobject instance) {

    pthread_mutex_init(&list_lock, NULL);
    pthread_mutex_lock(&list_lock);

    sentinel.count = 0;

    pthread_mutex_unlock(&list_lock);

    return true;
}

//Table clients for variables declared in OPC UA variables field.
static UA_Full_Client *clientsTable;

//Client for write Elcometer variable array.
static UA_Full_Client elcometerClient;

//Client for write Elcometer variable array.
static UA_Full_Client actualElcometerClient;

static UA_Full_Client alertVariableClient;

static UA_Full_Client infoVariableClient;

static UA_Full_Client scanVariableClient;

static UA_Full_Client loginVariableClient;


void printNode(int position)
{
    /* Same thing, this time using the node iterator... */
    UA_NodeId *parent = UA_NodeId_new();
    *parent = UA_NODEID_NUMERIC(0, UA_NS0ID_OBJECTSFOLDER);
    UA_Client_forEachChildNodeCall(clientsTable[position].client, UA_NODEID_NUMERIC(0, UA_NS0ID_OBJECTSFOLDER),
                                   nodeIter, (void *) parent);
    UA_NodeId_delete(parent);
}


//---------------------End additional functions.

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cCreateClients(JNIEnv *env,
                                                                              jobject instance,
                                                                              jint size){
    int i = 0;
    //Create watch variables clients.
    clientsTable = (UA_Full_Client*) malloc(size * sizeof(UA_Full_Client));
    for(i = 0; i < size; ++i)
    {
        clientsTable[i].client = UA_Client_new(UA_ClientConfig_standard);
    }
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetEndpoint(JNIEnv *env,
                                                                            jobject instance,
                                                                            jint position,
                                                                            jstring endpoints_) {
    clientsTable[position].endpoint = strdup((*env)->GetStringUTFChars(env, endpoints_, 0));
}


JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetNode(JNIEnv *env,
                                                                        jobject instance,
                                                                        jint position,
                                                                        jint nodeID,
                                                                        jstring nodeNames_) {
    clientsTable[position].nodeName = strdup((*env)->GetStringUTFChars(env, nodeNames_, 0));
    clientsTable[position].nodeID = nodeID;
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetVariablesType(JNIEnv *env,
                                                                                 jobject instance,
                                                                                 jint position,
                                                                                 jstring variableNames_) {
    clientsTable[position].variable = parseType((*env)->GetStringUTFChars(env, variableNames_, 0));

}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cConnect(JNIEnv *env,
                                                                        jobject instance,
                                                                        jint position) {
    UA_StatusCode statusCode = UA_Client_connect(clientsTable[position].client, clientsTable[position].endpoint);
    if(statusCode != UA_STATUSCODE_GOOD) {
        return false;
    }else return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cRead(JNIEnv *env, jobject instance,
                                                                    jint position) {
    /* Read attribute */
    clientsTable[position].status = UA_Client_readValueAttribute(clientsTable[position].client, UA_NODEID_STRING(clientsTable[position].nodeID, clientsTable[position].nodeName), clientsTable[position].variable);
    if(clientsTable[position].status != UA_STATUSCODE_GOOD)
    {
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cWrite(JNIEnv *env, jobject instance,
                                                                     jint position) {

    clientsTable[position].status = UA_Client_writeValueAttribute(clientsTable[position].client, UA_NODEID_STRING(clientsTable[position].nodeID, clientsTable[position].nodeName), clientsTable[position].variable);
    if(clientsTable[position].status != UA_STATUSCODE_GOOD)
    {
        return false;
    }
    return true;

}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cClearMemory(JNIEnv *env,
                                                                           jobject instance,
                                                                           jint size) {
    int i;
    for(i = 0; i < size; ++i)
    {
        //Clear fields.
        free(clientsTable[i].nodeName);
        free(clientsTable[i].endpoint);
        UA_Variant_delete(clientsTable[i].variable);
        UA_Client_delete(clientsTable[i].client);
    }
    //Clear all table.
    if(clientsTable != NULL) {
        free(clientsTable);
    }
}


void cClearElcometerOpcUaClient(void) {
    //Clear fields.
    free(elcometerClient.nodeName);
    free(elcometerClient.endpoint);
    UA_Variant_delete(elcometerClient.variable);
}

//Get data functions.
JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetVariableBoolean(JNIEnv *env, jobject instance,
                                                                     jint position) {

    return (jboolean)*(UA_Boolean*)(clientsTable[position].variable->data);
}


JNIEXPORT jint JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetVariableInteger(JNIEnv *env, jobject instance,
                                                                                   jint position) {

    return (jint)*(UA_Int32*)(clientsTable[position].variable->data);
}


JNIEXPORT jint JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cCheckReadStatus(JNIEnv *env, jobject instance,
                                                                    jint position) {
    /* Read attribute */
    UA_StatusCode statusCode = UA_Client_readValueAttribute(clientsTable[position].client, UA_NODEID_STRING(clientsTable[position].nodeID, clientsTable[position].nodeName), clientsTable[position].variable);
    printf("%d\n", statusCode);
    return statusCode;
}

JNIEXPORT jfloat JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetVariableFloat(JNIEnv *env,
                                                                                 jobject instance,
                                                                                 jint position) {

    return (jfloat)*(UA_Float *)(clientsTable[position].variable->data);

}

JNIEXPORT jstring JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetVariableString(JNIEnv *env,
                                                                                  jobject instance,
                                                                                  jint position) {
    //TODO: Correct string getter.
    UA_String* uaString = clientsTable[position].variable->data;
    char* convert = (char*)UA_malloc(sizeof(char)*uaString->length+1);
    memcpy(convert, uaString->data, uaString->length );
    convert[uaString->length] = '\0';

    return (*env)->NewStringUTF(env, convert);
}

JNIEXPORT jlong JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetVariableLong(JNIEnv *env,
                                                                                jobject instance,
                                                                                jint position) {

    return (jlong)*(UA_Int64 *)(clientsTable[position].variable->data);

}

//Set data functions.
JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetVariableBoolean(JNIEnv *env, jobject instance,
                                                                                   jint position, jboolean value) {
    UA_Boolean boolean_variable = value;
    UA_Variant_setScalarCopy(clientsTable[position].variable, &boolean_variable, &UA_TYPES[UA_TYPES_BOOLEAN]);
}


JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetVariableInteger(JNIEnv *env, jobject instance,
                                                                                   jint position, jint value) {
    UA_Int32 integer_variable = value;
    UA_Variant_setScalarCopy(clientsTable[position].variable, &integer_variable, &UA_TYPES[UA_TYPES_INT32]);
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetVariableShort(JNIEnv *env, jobject instance,
                                                                                   jint position, jint value) {
    UA_Int16 integer_variable = value;
    UA_Variant_setScalarCopy(clientsTable[position].variable, &integer_variable, &UA_TYPES[UA_TYPES_INT16]);
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetVariableFloat(JNIEnv *env,
                                                                                 jobject instance,
                                                                                 jint position, jfloat value) {

    UA_Float float_variable = value;
    UA_Variant_setScalarCopy(clientsTable[position].variable, &float_variable, &UA_TYPES[UA_TYPES_FLOAT]);
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetVariableString(JNIEnv *env,
                                                                                  jobject instance,
                                                                                  jint position, jstring value) {

    UA_String string_variable = UA_STRING(strdup((*env)->GetStringUTFChars(env, value, 0)));
    UA_Variant_setScalarCopy(clientsTable[position].variable, &string_variable, &UA_TYPES[UA_TYPES_STRING]);}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetVariableLong(JNIEnv *env,
                                                                                jobject instance,
                                                                                jint position, jlong value) {
    UA_Int64 long_variable = value;
    UA_Variant_setScalarCopy(clientsTable[position].variable, &long_variable, &UA_TYPES[UA_TYPES_INT64]);
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cInitElcometerArrayWriting(
        JNIEnv *env, jobject instance, jstring endpoint_, jint node, jstring nodeName_) {

    //Init client.
    elcometerClient.client = UA_Client_new(UA_ClientConfig_standard);
    elcometerClient.endpoint = strdup((*env)->GetStringUTFChars(env, endpoint_, 0));
    elcometerClient.nodeID = node;

    UA_Variant * myVariant = UA_Variant_new();
    UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_FLOAT]);
    elcometerClient.variable = myVariant;

    elcometerClient.nodeName = strdup((*env)->GetStringUTFChars(env, nodeName_, 0));

    UA_StatusCode statusCode = UA_Client_connect(elcometerClient.client, elcometerClient.endpoint);
    if(statusCode != UA_STATUSCODE_GOOD) {
        return false;
    }else return true;

}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cWriteToElcometerArray(JNIEnv *env,
                                                                                     jobject instance,
                                                                                     jfloatArray measures_,
                                                                                     jint size) {


    jfloat *measures = (*env)->GetFloatArrayElements(env, measures_, NULL);

    //Modify node name.
    UA_Variant_setArray( elcometerClient.variable, measures, (size_t) size, &UA_TYPES[UA_TYPES_FLOAT]);
    elcometerClient.status = UA_Client_writeValueAttribute(elcometerClient.client, UA_NODEID_STRING(elcometerClient.nodeID, elcometerClient.nodeName), elcometerClient.variable);
    if(elcometerClient.status != UA_STATUSCODE_GOOD)
    {
        cClearElcometerOpcUaClient();
        return false;
    }

    cClearElcometerOpcUaClient();
    (*env)->ReleaseFloatArrayElements(env, measures_, measures, 0);

    return true;
}

JNIEXPORT jint JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetVariableStatus(JNIEnv *env,
                                                                                 jobject instance,
                                                                                 jint position) {

    return clientsTable[position].status;

}


JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cInitElcometerWriting(
        JNIEnv *env, jobject instance, jstring endpoint_, jint node, jstring nodeName_) {

    //Init client.
    actualElcometerClient.client = UA_Client_new(UA_ClientConfig_standard);
    actualElcometerClient.endpoint = strdup((*env)->GetStringUTFChars(env, endpoint_, 0));
    actualElcometerClient.nodeID = node;

    UA_Variant * myVariant = UA_Variant_new();
    UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_FLOAT]);
    actualElcometerClient.variable = myVariant;

    actualElcometerClient.nodeName = strdup((*env)->GetStringUTFChars(env, nodeName_, 0));

    UA_StatusCode statusCode = UA_Client_connect(actualElcometerClient.client, actualElcometerClient.endpoint);
    if(statusCode != UA_STATUSCODE_GOOD) {
        return false;
    }else return true;

}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cWriteToActualElcometer(JNIEnv *env,
                                                                                     jobject instance,
                                                                                      jfloat value) {
    UA_Float float_variable = value;
    UA_Variant_setScalarCopy(actualElcometerClient.variable, &float_variable, &UA_TYPES[UA_TYPES_FLOAT]);
    //Modify node name.
    actualElcometerClient.status = UA_Client_writeValueAttribute(actualElcometerClient.client, UA_NODEID_STRING(actualElcometerClient.nodeID, actualElcometerClient.nodeName), actualElcometerClient.variable);
    if(actualElcometerClient.status != UA_STATUSCODE_GOOD)
    {
        return false;
    }

    return true;
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cClearElcometerArrayClient(
        JNIEnv *env, jobject instance) {

    UA_Variant_delete(elcometerClient.variable);
    UA_Client_delete(elcometerClient.client);
    free(elcometerClient.nodeName);
    free(elcometerClient.endpoint);

}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cClearActualElcometerClient(
        JNIEnv *env, jobject instance) {

    UA_Variant_delete(actualElcometerClient.variable);
    UA_Client_delete(actualElcometerClient.client);
    free(actualElcometerClient.nodeName);
    free(actualElcometerClient.endpoint);

}

UA_Client_List_Node * createNewNode()
{
    UA_Client_List_Node * newNode =  (UA_Client_List_Node*) malloc(sizeof(UA_Client_List_Node));

    newNode->node =  (UA_Full_Client*) malloc(sizeof(UA_Full_Client));
    newNode->node->client =  UA_Client_new(UA_ClientConfig_standard);
    //Add node to end of list.
    newNode->next = NULL;
    newNode->prev = sentinel.tail;
    sentinel.tail = newNode;

    ++sentinel.count;
    if(newNode->prev == NULL)
        sentinel.head = newNode;
    else
        (newNode->prev)->next = newNode;
    return newNode;
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cConnectWithUndefiniedVariable(
        JNIEnv *env, jobject instance, jstring endpoint_, jint node, jstring nodeName_, jint position) {

    const char *endpoint = (*env)->GetStringUTFChars(env, endpoint_, 0);
    const char *nodeName = (*env)->GetStringUTFChars(env, nodeName_, 0);

    //Init client.

    pthread_mutex_lock(&list_lock);
    UA_Client_List_Node * newNode = createNewNode();

    newNode->node->endpoint = strdup((*env)->GetStringUTFChars(env, endpoint_, 0));
    newNode->node->nodeID = node;
    newNode->number = position;
    newNode->node->nodeName = strdup((*env)->GetStringUTFChars(env, nodeName_, 0));

    (*env)->ReleaseStringUTFChars(env, endpoint_, endpoint);
    (*env)->ReleaseStringUTFChars(env, nodeName_, nodeName);

    UA_StatusCode statusCode = UA_Client_connect(newNode->node->client, newNode->node->endpoint);
    if(statusCode != UA_STATUSCODE_GOOD) {
        pthread_mutex_unlock(&list_lock);
        return false;
    }else {
        pthread_mutex_unlock(&list_lock);
        return true;
    }
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cAddUndefiniedIntVariable(
        JNIEnv *env, jobject instance,  jint position) {

    UA_Variant * myVariant = UA_Variant_new();
    UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_INT32]);

    //Lock mutex.

    //Delete node.
    pthread_mutex_lock(&list_lock);

    UA_Client_List_Node * temp = sentinel.head;
    //Find good node.
    if(temp == NULL)
        return;
    while(temp->next != NULL && temp->number != position)
    {
        temp = temp->next;
    }
    temp->node->variable = myVariant;
    pthread_mutex_unlock(&list_lock);

}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cAddUndefiniedBoolVariable(
        JNIEnv *env, jobject instance, jint position) {

    UA_Variant * myVariant = UA_Variant_new();
    UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_BOOLEAN]);

    //Lock mutex.
    pthread_mutex_lock(&list_lock);

    UA_Client_List_Node * temp = sentinel.head;
    //Find good node.
    if(temp == NULL)
        return;
    while(temp->next != NULL && temp->number != position)
    {
        temp = temp->next;
    }

    temp->node->variable = myVariant;
    //Add number.
    pthread_mutex_unlock(&list_lock);
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cAddUndefiniedFloatVariable(
        JNIEnv *env, jobject instance, jint position) {

    UA_Variant * myVariant = UA_Variant_new();
    UA_Variant_setScalar(myVariant, NULL, &UA_TYPES[UA_TYPES_FLOAT]);

    //Lock mutex.
    pthread_mutex_lock(&list_lock);

    UA_Client_List_Node * temp = sentinel.head;

    if(temp == NULL)
    {
        pthread_mutex_unlock(&list_lock);
        return;
    }
    //Find good node.
    while(temp->next != NULL && temp->number != position)
    {
        temp = temp->next;
    }
    temp->node->variable = myVariant;
    //Add number.
    pthread_mutex_unlock(&list_lock);
}

JNIEXPORT jint JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetUndefiniedIntVariable(JNIEnv *env,
                                                                                        jobject instance, jint position) {

    pthread_mutex_lock(&list_lock);

    UA_Client_List_Node * temp = sentinel.head;
    //Find good node.
    if(temp == NULL)
    {
        pthread_mutex_unlock(&list_lock);
        return -999;
    }
    while(temp->next != NULL && temp->number != position)
    {
        temp = temp->next;
    }

    jint ret = (jint)*(UA_Int32 *)(temp->node->variable->data);

    return ret;
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetUndefiniedBoolVariable(
        JNIEnv *env, jobject instance, jint position) {

    pthread_mutex_lock(&list_lock);

    UA_Client_List_Node *temp = sentinel.head;
    //Find good node.
    if (temp == NULL) {
        pthread_mutex_unlock(&list_lock);
        return false;
    }
    while(temp->next != NULL && temp->number != position)
    {
        temp = temp->next;
    }

    jboolean ret = (jboolean)*(UA_Boolean*)(temp->node->variable->data);
    pthread_mutex_unlock(&list_lock);

    return ret;
}

JNIEXPORT jfloat JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetUndefiniedFloatVariable(
        JNIEnv *env, jobject instance, jint position) {


    pthread_mutex_lock(&list_lock);

    UA_Client_List_Node * temp = sentinel.head;
    //Find good node.
    while(temp->next != NULL && temp->number != position)
    {
        temp = temp->next;
    }

    if(temp->number != position)
    {
        pthread_mutex_unlock(&list_lock);
        return -99999.0;
    }


    jfloat ret = (jfloat)*(UA_Float *)(temp->node->variable->data);
    pthread_mutex_unlock(&list_lock);

    return ret;

}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cReadUndefiniedVariable(JNIEnv *env,
                                                                                      jobject instance, jint position) {

    /* Read attribute */

    pthread_mutex_lock(&list_lock);

    UA_Client_List_Node * temp = sentinel.head;

    if(temp == NULL)
    {
        pthread_mutex_unlock(&list_lock);
        return false;
    }
    //Find good node.
    while(temp->next != NULL && temp->number != position)
    {
        temp = temp->next;
    }

    if(temp->number != position)
    {
        pthread_mutex_unlock(&list_lock);
        return false;
    }

    temp->node->status = UA_Client_readValueAttribute(temp->node->client, UA_NODEID_STRING(temp->node->nodeID, temp->node->nodeName), temp->node->variable);


    if(temp->node->status != UA_STATUSCODE_GOOD) {
        pthread_mutex_unlock(&list_lock);
        return false;
    }else {
        pthread_mutex_unlock(&list_lock);
        return true;
    }


}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cCleanUndefiniedVariable(JNIEnv *env,
                                                                                       jobject instance,
                                                                                       jint position) {

    //Delete node.
    pthread_mutex_lock(&list_lock);

    UA_Client_List_Node * temp = sentinel.head;
    //Find good node.
    while(temp->next != NULL && temp->number != position)
    {
        temp = temp->next;
    }
    if(temp->number != position) {
        pthread_mutex_unlock(&list_lock);
        return;
    }
    //Delete tem from node.
    temp->prev->next = temp->next;
    temp->next->prev = temp->prev;

    pthread_mutex_unlock(&list_lock);

    if(temp != NULL) {
        UA_Client_disconnect(temp->node->client);
        UA_Client_delete(temp->node->client);
        UA_Variant_delete(temp->node->variable);
        pthread_mutex_unlock(&list_lock);
        free(temp->node->nodeName);
        free(temp->node->endpoint);
        free(temp);
    }
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cDeleteClientList(JNIEnv *env,
                                                                                jobject instance) {


    pthread_mutex_lock(&list_lock);

    UA_Client_List_Node * temp = sentinel.head;
    //Find good node.
    while(temp->next != NULL)
    {
        UA_Client_disconnect(temp->node->client);
        UA_Client_delete(temp->node->client);
        UA_Variant_delete(temp->node->variable);
        pthread_mutex_unlock(&list_lock);
        free(temp->node->nodeName);
        free(temp->node->endpoint);
        temp = temp->next;
        free(temp->prev);
    }
    sentinel.head = NULL;
    sentinel.tail = NULL;

    pthread_mutex_unlock(&list_lock);
    pthread_mutex_destroy(&list_lock);


}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cInitAlertVariableReading(JNIEnv *env,
                                                                                        jobject instance,
                                                                                        jstring endpoint_,
                                                                                        jint node,
                                                                                        jstring nodeName_) {
    /* Write node attribute (using the highlevel API) */
    alertVariableClient.variable = UA_Variant_new();
    alertVariableClient.client = UA_Client_new(UA_ClientConfig_standard);
    /* Choose type of variable */
    UA_Variant_setScalar(alertVariableClient.variable, NULL, &UA_TYPES[UA_TYPES_BOOLEAN]);

    alertVariableClient.endpoint = strdup((*env)->GetStringUTFChars(env, endpoint_, 0));
    alertVariableClient.nodeName = strdup((*env)->GetStringUTFChars(env, nodeName_, 0));
    alertVariableClient.nodeID = node;

    UA_StatusCode statusCode = UA_Client_connect(alertVariableClient.client, alertVariableClient.endpoint);
    if(statusCode != UA_STATUSCODE_GOOD) {
        return false;
    }else return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cReadAlertVariable(JNIEnv *env,
                                                                                 jobject instance) {

    /* Read attribute */
    alertVariableClient.status = UA_Client_readValueAttribute(alertVariableClient.client, UA_NODEID_STRING(alertVariableClient.nodeID, alertVariableClient.nodeName), alertVariableClient.variable);
    if(alertVariableClient.status != UA_STATUSCODE_GOOD)
    {
        return false;
    }
    return true;

}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetAlertValue(JNIEnv *env,
                                                                             jobject instance) {

    return (jboolean)*(UA_Boolean*)(alertVariableClient.variable->data);

}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cCleanAlertVariable(JNIEnv *env,
                                                                                  jobject instance) {

    UA_Variant_delete(alertVariableClient.variable);
    UA_Client_delete(alertVariableClient.client);
    free(alertVariableClient.nodeName);
    free(alertVariableClient.endpoint);

}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cInitInfoVariableReading(JNIEnv *env,
                                                                                       jobject instance,
                                                                                       jstring endpoint_,
                                                                                       jint node,
                                                                                       jstring nodeName_) {

    /* Write node attribute (using the highlevel API) */
    infoVariableClient.variable = UA_Variant_new();
    infoVariableClient.client = UA_Client_new(UA_ClientConfig_standard);
    /* Choose type of variable */
    UA_Variant_setScalar(infoVariableClient.variable, NULL, &UA_TYPES[UA_TYPES_BOOLEAN]);

    infoVariableClient.endpoint = strdup((*env)->GetStringUTFChars(env, endpoint_, 0));
    infoVariableClient.nodeName = strdup((*env)->GetStringUTFChars(env, nodeName_, 0));
    infoVariableClient.nodeID = node;

    UA_StatusCode statusCode = UA_Client_connect(infoVariableClient.client, infoVariableClient.endpoint);
    if(statusCode != UA_STATUSCODE_GOOD) {
        return false;
    }else return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cReadInfoVariable(JNIEnv *env,
                                                                                jobject instance) {

    /* Read attribute */
    infoVariableClient.status = UA_Client_readValueAttribute(infoVariableClient.client, UA_NODEID_STRING(infoVariableClient.nodeID, infoVariableClient.nodeName), infoVariableClient.variable);
    if(infoVariableClient.status != UA_STATUSCODE_GOOD)
    {
        return false;
    }
    return true;
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cCleanInfoVariable(JNIEnv *env,
                                                                                 jobject instance) {
    UA_Variant_delete(infoVariableClient.variable);
    UA_Client_delete(infoVariableClient.client);
    free(infoVariableClient.nodeName);
    free(infoVariableClient.endpoint);

}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cGetInfoValue(JNIEnv *env,
                                                                            jobject instance) {

    return (jboolean)*(UA_Boolean*)(infoVariableClient.variable->data);

}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cInitScanVariableWriting(JNIEnv *env,
                                                                                        jobject instance,
                                                                                        jstring endpoint_,
                                                                                        jint node,
                                                                                        jstring nodeName_) {
    /* Write node attribute (using the highlevel API) */
    scanVariableClient.variable = UA_Variant_new();
    scanVariableClient.client = UA_Client_new(UA_ClientConfig_standard);
    /* Choose type of variable */
    UA_Variant_setScalar(scanVariableClient.variable, NULL, &UA_TYPES[UA_TYPES_STRING]);

    scanVariableClient.endpoint = strdup((*env)->GetStringUTFChars(env, endpoint_, 0));
    scanVariableClient.nodeName = strdup((*env)->GetStringUTFChars(env, nodeName_, 0));
    scanVariableClient.nodeID = node;

    UA_StatusCode statusCode = UA_Client_connect(scanVariableClient.client, scanVariableClient.endpoint);
    if(statusCode != UA_STATUSCODE_GOOD) {
        return false;
    }else return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cWriteScanVariable(JNIEnv *env,
                                                                                  jobject instance) {

    scanVariableClient.status = UA_Client_writeValueAttribute(scanVariableClient.client, UA_NODEID_STRING(scanVariableClient.nodeID, scanVariableClient.nodeName), scanVariableClient.variable);
    if(scanVariableClient.status != UA_STATUSCODE_GOOD)
    {
        return false;
    }
    return true;

}


JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cCleanScanVariable(JNIEnv *env,
                                                                                  jobject instance) {

    UA_Variant_delete(scanVariableClient.variable);
    UA_Client_delete(scanVariableClient.client);
    free(scanVariableClient.nodeName);
    free(scanVariableClient.endpoint);
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetScanVariable(JNIEnv *env,
                                                                             jobject instance,
                                                                             jstring scanString_) {

    UA_String string_variable = UA_STRING(strdup((*env)->GetStringUTFChars(env, scanString_, 0)));
    UA_Variant_setScalarCopy(scanVariableClient.variable, &string_variable, &UA_TYPES[UA_TYPES_STRING]);
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cInitLoginVariableWriting(
        JNIEnv *env, jobject instance, jstring endpoint_, jint node, jstring nodeName_) {

    /* Write node attribute (using the highlevel API) */
    loginVariableClient.variable = UA_Variant_new();
    loginVariableClient.client = UA_Client_new(UA_ClientConfig_standard);
    /* Choose type of variable */
    UA_Variant_setScalar(loginVariableClient.variable, NULL, &UA_TYPES[UA_TYPES_STRING]);

    loginVariableClient.endpoint = strdup((*env)->GetStringUTFChars(env, endpoint_, 0));
    loginVariableClient.nodeName = strdup((*env)->GetStringUTFChars(env, nodeName_, 0));
    loginVariableClient.nodeID = node;

    UA_StatusCode statusCode = UA_Client_connect(loginVariableClient.client, loginVariableClient.endpoint);
    if(statusCode != UA_STATUSCODE_GOOD) {
        return false;
    }else return true;
}

JNIEXPORT jboolean JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cWriteLoginVariable(JNIEnv *env,
                                                                                   jobject instance) {

    loginVariableClient.status = UA_Client_writeValueAttribute(loginVariableClient.client, UA_NODEID_STRING(loginVariableClient.nodeID, loginVariableClient.nodeName), loginVariableClient.variable);
    if(loginVariableClient.status != UA_STATUSCODE_GOOD)
    {
        return false;
    }
    return true;
}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cSetLoginVariable(JNIEnv *env,
                                                                                 jobject instance,
                                                                                 jstring login_) {
    UA_String string_variable = UA_STRING(strdup((*env)->GetStringUTFChars(env, login_, 0)));
    UA_Variant_setScalarCopy(loginVariableClient.variable, &string_variable, &UA_TYPES[UA_TYPES_STRING]);

}

JNIEXPORT void JNICALL
Java_com_sciteex_ssip_sciteexmeasurementmanager_JNIOpcUaClient_cCleanLoginVariable(JNIEnv *env,
                                                                                    jobject instance) {

    UA_Variant_delete(loginVariableClient.variable);
    UA_Client_delete(loginVariableClient.client);
    free(loginVariableClient.nodeName);
    free(loginVariableClient.endpoint);
}